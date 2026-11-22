import os
import time
import uuid
import logging
import threading
import glob
from typing import Dict, Any, Optional
import yt_dlp

from models import TaskStatus, VideoMetadata, FormatInfo, SubtitleTrack, DownloadRequest

logger = logging.getLogger("yt_dlp_server")
logging.basicConfig(level=logging.INFO)

DOWNLOAD_DIR = os.path.abspath(os.environ.get("DOWNLOAD_DIR", "/app/downloads"))
os.makedirs(DOWNLOAD_DIR, exist_ok=True)

COOKIE_FILE = os.path.abspath(os.environ.get("COOKIE_FILE", os.path.join(os.path.dirname(__file__), "cookies.txt")))
USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
if os.environ.get("YOUTUBE_COOKIES"):
    try:
        with open(COOKIE_FILE, "w") as f:
            f.write(os.environ.get("YOUTUBE_COOKIES"))
        logger.info("Loaded YOUTUBE_COOKIES from environment variable.")
    except Exception as e:
        logger.warning(f"Failed to write YOUTUBE_COOKIES: {e}")

# In-memory task repository
tasks: Dict[str, TaskStatus] = {}
tasks_lock = threading.Lock()

def format_bytes(size: Optional[int]) -> str:
    if not size:
        return "0 B"
    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if size < 1024.0:
            return f"{size:.2f} {unit}"
        size /= 1024.0
    return f"{size:.2f} PB"

def format_eta(seconds: Any) -> str:
    if not seconds:
        return ""
    try:
        sec = int(round(float(seconds)))
        if sec <= 0:
            return "1s"
        m, s = divmod(sec, 60)
        h, m = divmod(m, 60)
        if h > 0:
            return f"{h}h {m}m {s}s"
        if m > 0:
            return f"{m}m {s}s"
        return f"{s}s"
    except Exception:
        return ""

def clean_url(url: str) -> str:
    """If user pastes a YouTube watch link with an auto-generated radio mix or playlist, strip extra playlist params."""
    if not url:
        return url
    try:
        import urllib.parse
        parsed = urllib.parse.urlparse(url.strip())
        netloc = parsed.netloc.lower()
        if "youtu.be" in netloc:
            video_id = parsed.path.strip("/")
            if video_id:
                return f"https://www.youtube.com/watch?v={video_id}"
        elif "youtube.com" in netloc and "/watch" in parsed.path:
            qs = urllib.parse.parse_qs(parsed.query)
            if 'v' in qs:
                video_id = qs['v'][0]
                list_id = qs.get('list', [None])[0]
                if list_id and not list_id.startswith("RD"):
                    return f"https://www.youtube.com/watch?v={video_id}&list={list_id}"
                return f"https://www.youtube.com/watch?v={video_id}"
    except Exception:
        pass
    return url.strip()

def extract_info(url: str) -> VideoMetadata:
    """Extract metadata and format list without downloading."""
    clean = clean_url(url)
    ydl_opts: Dict[str, Any] = {
        'skip_download': True,
        'noplaylist': True,
        'quiet': True,
        'no_warnings': True,
        'socket_timeout': 15,
        'user_agent': USER_AGENT,
        'http_headers': {'User-Agent': USER_AGENT},
        'extractor_args': {'youtube': {'player_client': ['visionos', 'ios', 'web_creator', 'mweb', 'web']}},
        'js_runtimes': {'node': {}},
    }
    if os.path.exists(COOKIE_FILE) and os.path.getsize(COOKIE_FILE) > 10:
        ydl_opts['cookiefile'] = COOKIE_FILE
    
    info = None
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(clean, download=False)
    except Exception as e:
        err_str = str(e)
        logger.warning(f"Default extraction error: {err_str}. Trying fallback player clients...")
        for clients in [['visionos'], ['ios'], ['tv'], ['mweb', 'web_creator'], ['android']]:
            try:
                fallback_opts = dict(ydl_opts)
                fallback_opts.pop('cookiefile', None)
                fallback_opts['extractor_args'] = {'youtube': {'player_client': clients}}
                with yt_dlp.YoutubeDL(fallback_opts) as ydl:
                    info = ydl.extract_info(clean, download=False)
                if info:
                    break
            except Exception:
                continue
        if not info:
            raise e
        
    # Flatten playlist if user passed a playlist or single video
    if 'entries' in info and info['entries']:
        info = info['entries'][0]

    formats_list = []
    raw_formats = info.get('formats', [])
    for f in raw_formats:
        vcodec = f.get('vcodec')
        acodec = f.get('acodec')
        has_video = vcodec is not None and vcodec != 'none'
        has_audio = acodec is not None and acodec != 'none'
        
        resolution = f.get('resolution')
        if not resolution:
            if f.get('width') and f.get('height'):
                resolution = f"{f['width']}x{f['height']}"
            elif has_audio and not has_video:
                resolution = "audio only"
            else:
                resolution = "unknown"

        formats_list.append(FormatInfo(
            format_id=str(f.get('format_id')),
            ext=f.get('ext', ''),
            resolution=resolution,
            width=f.get('width'),
            height=f.get('height'),
            fps=f.get('fps'),
            vcodec=vcodec,
            acodec=acodec,
            filesize=f.get('filesize'),
            filesize_approx=f.get('filesize_approx'),
            tbr=f.get('tbr'),
            format_note=f.get('format_note'),
            has_video=has_video,
            has_audio=has_audio
        ))

    subtitles = []
    for lang, tracks in info.get('subtitles', {}).items():
        name = tracks[0].get('name') if tracks else lang
        ext = tracks[0].get('ext') if tracks else 'vtt'
        subtitles.append(SubtitleTrack(lang=lang, name=name, ext=ext))

    auto_caps = []
    for lang, tracks in info.get('automatic_captions', {}).items():
        name = tracks[0].get('name') if tracks else lang
        ext = tracks[0].get('ext') if tracks else 'vtt'
        auto_caps.append(SubtitleTrack(lang=lang, name=name, ext=ext))

    return VideoMetadata(
        id=info.get('id', ''),
        title=info.get('title', 'Untitled'),
        description=info.get('description', ''),
        uploader=info.get('uploader', ''),
        channel=info.get('channel', ''),
        duration=info.get('duration', 0),
        duration_string=info.get('duration_string', ''),
        thumbnail=info.get('thumbnail', ''),
        view_count=info.get('view_count', 0),
        webpage_url=info.get('webpage_url', url),
        formats=formats_list,
        subtitles=subtitles,
        automatic_captions=auto_caps
    )

def _progress_hook(d: Dict[str, Any], task_id: str):
    with tasks_lock:
        task = tasks.get(task_id)
        if not task:
            return

        status = d.get('status')
        if status == 'downloading':
            task.status = 'downloading'
            total_bytes = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
            downloaded = d.get('downloaded_bytes', 0)
            task.total_bytes = total_bytes
            task.downloaded_bytes = downloaded
            if total_bytes > 0:
                task.progress = round((downloaded / total_bytes) * 100.0, 2)
            
            speed = d.get('speed')
            if speed:
                task.speed = speed
                task.speed_str = f"{format_bytes(int(speed))}/s"
            
            eta = d.get('eta')
            if eta:
                task.eta = eta
                task.eta_str = format_eta(eta)

        elif status == 'finished':
            task.status = 'processing'
            task.progress = 100.0

def _postprocessor_hook(d: Dict[str, Any], task_id: str):
    with tasks_lock:
        task = tasks.get(task_id)
        if not task:
            return
        status = d.get('status')
        if status == 'started':
            task.status = 'processing'

def execute_download(task_id: str, req: DownloadRequest):
    outtmpl = os.path.join(DOWNLOAD_DIR, f"{task_id}_%(title).150B.%(ext)s")
    
    postprocessors = []

    # 1. SponsorBlock
    if req.sponsorblock_remove and len(req.sponsorblock_remove) > 0:
        postprocessors.append({
            'key': 'SponsorBlock',
            'categories': req.sponsorblock_remove,
            'when': 'pre_process'
        })
        postprocessors.append({
            'key': 'ModifyChapters',
            'remove_sponsor_segments': req.sponsorblock_remove
        })

    # 2. Audio Extraction
    if req.audio_only:
        postprocessors.append({
            'key': 'FFmpegExtractAudio',
            'preferredcodec': req.audio_format or 'mp3',
            'preferredquality': req.audio_quality or '320',
        })

    # 3. Metadata & Chapters
    if req.embed_metadata:
        postprocessors.append({
            'key': 'FFmpegMetadata',
            'add_chapters': True,
            'add_metadata': True,
        })

    # 4. Embed Subtitles
    if req.embed_subtitles:
        postprocessors.append({
            'key': 'FFmpegEmbedSubtitle',
            'already_have_subtitle': False
        })

    # 5. Embed Thumbnail
    if req.embed_thumbnail:
        postprocessors.append({
            'key': 'EmbedThumbnail',
            'already_have_thumbnail': False
        })

    ydl_opts: Dict[str, Any] = {
        'outtmpl': outtmpl,
        'format': req.format_selector or 'bestvideo*+bestaudio/best',
        'noplaylist': True,
        'socket_timeout': 20,
        'user_agent': USER_AGENT,
        'http_headers': {'User-Agent': USER_AGENT},
        'extractor_args': {'youtube': {'player_client': ['visionos', 'ios', 'web_creator', 'mweb', 'web']}},
        'merge_output_format': req.merge_output_format if not req.audio_only else None,
        'writesubtitles': req.embed_subtitles,
        'writeautomaticsub': req.embed_subtitles,
        'subtitleslangs': req.subtitles_langs if req.subtitles_langs else ['en.*', 'all'],
        'writethumbnail': req.embed_thumbnail,
        'progress_hooks': [lambda d: _progress_hook(d, task_id)],
        'postprocessor_hooks': [lambda d: _postprocessor_hook(d, task_id)],
        'postprocessors': postprocessors,
        'quiet': False,
        'no_warnings': False,
    }

    if os.path.exists(COOKIE_FILE) and os.path.getsize(COOKIE_FILE) > 10:
        ydl_opts['cookiefile'] = COOKIE_FILE
    ydl_opts['js_runtimes'] = {'node': {}}

    # Range trimming (clips)
    if req.clip_start or req.clip_end:
        def time_to_sec(t_str: str) -> float:
            parts = [float(p) for p in t_str.split(':')]
            if len(parts) == 3:
                return parts[0]*3600 + parts[1]*60 + parts[2]
            elif len(parts) == 2:
                return parts[0]*60 + parts[1]
            return parts[0]

        start_sec = time_to_sec(req.clip_start) if req.clip_start else 0
        end_sec = time_to_sec(req.clip_end) if req.clip_end else float('inf')
        
        ydl_opts['download_ranges'] = yt_dlp.utils.download_range_func(None, [(start_sec, end_sec)])
        ydl_opts['force_keyframes_at_cuts'] = True

    # User extra custom options override
    if req.custom_options:
        ydl_opts.update(req.custom_options)

    try:
        target_url = clean_url(req.url)
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Extract info to get title first
            info = ydl.extract_info(target_url, download=True)
            if 'entries' in info and info['entries']:
                info = info['entries'][0]

            with tasks_lock:
                task = tasks.get(task_id)
                if task:
                    task.title = info.get('title', 'Media Download')

        # Locate output file matching task_id prefix
        matched_files = glob.glob(os.path.join(DOWNLOAD_DIR, f"{task_id}_*"))
        if matched_files:
            final_file = matched_files[0]
            with tasks_lock:
                task = tasks.get(task_id)
                if task:
                    task.status = 'completed'
                    task.progress = 100.0
                    task.file_path = final_file
                    task.filename = os.path.basename(final_file)
        else:
            raise FileNotFoundError("Output file was not found after yt-dlp execution.")

    except Exception as e:
        logger.error(f"Download failed for task {task_id}: {str(e)}", exc_info=True)
        with tasks_lock:
            task = tasks.get(task_id)
            if task:
                task.status = 'error'
                task.error = str(e)

def start_download_task(req: DownloadRequest) -> str:
    task_id = str(uuid.uuid4())[:8]
    task = TaskStatus(
        task_id=task_id,
        url=req.url,
        status="queued",
        progress=0.0
    )
    with tasks_lock:
        tasks[task_id] = task

    thread = threading.Thread(target=execute_download, args=(task_id, req), daemon=True)
    thread.start()
    return task_id

def cleanup_old_files(max_age_seconds: int = 7200):
    """Deletes downloads older than max_age_seconds (default 2 hours)."""
    now = time.time()
    for f in glob.glob(os.path.join(DOWNLOAD_DIR, "*")):
        try:
            if os.path.isfile(f) and (now - os.path.getmtime(f)) > max_age_seconds:
                os.remove(f)
                logger.info(f"Cleaned up stale file: {f}")
        except Exception as e:
            logger.warning(f"Failed to remove file {f}: {e}")

def run_auto_cleaner():
    while True:
        time.sleep(1800) # Check every 30 minutes
        cleanup_old_files()

# Start background cleaner thread
threading.Thread(target=run_auto_cleaner, daemon=True).start()
