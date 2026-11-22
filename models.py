from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class FormatInfo(BaseModel):
    format_id: str
    ext: str
    resolution: Optional[str] = None
    width: Optional[int] = None
    height: Optional[int] = None
    fps: Optional[float] = None
    vcodec: Optional[str] = None
    acodec: Optional[str] = None
    filesize: Optional[int] = None
    filesize_approx: Optional[int] = None
    tbr: Optional[float] = None
    format_note: Optional[str] = None
    has_video: bool = False
    has_audio: bool = False

class SubtitleTrack(BaseModel):
    lang: str
    name: Optional[str] = None
    ext: Optional[str] = None

class VideoMetadata(BaseModel):
    id: str
    title: str
    description: Optional[str] = ""
    uploader: Optional[str] = ""
    channel: Optional[str] = ""
    duration: Optional[int] = 0
    duration_string: Optional[str] = ""
    thumbnail: Optional[str] = ""
    view_count: Optional[int] = 0
    webpage_url: str
    formats: List[FormatInfo] = []
    subtitles: List[SubtitleTrack] = []
    automatic_captions: List[SubtitleTrack] = []

class ExtractRequest(BaseModel):
    url: str

class DownloadRequest(BaseModel):
    url: str
    # Format selector (e.g. "bestvideo+bestaudio/best", or "bv*[ext=mp4]+ba[ext=m4a]/b", or explicit format id)
    format_selector: Optional[str] = "bestvideo*+bestaudio/best"
    
    # Audio extraction options
    audio_only: bool = False
    audio_format: Optional[str] = "mp3" # mp3, m4a, flac, opus, wav
    audio_quality: Optional[str] = "320" # 320, 256, 192, 128
    
    # Post-processing options
    merge_output_format: Optional[str] = "mp4" # mp4, mkv, webm
    embed_thumbnail: bool = True
    embed_subtitles: bool = False
    subtitles_langs: Optional[List[str]] = None
    embed_metadata: bool = True
    
    # Sponsorblock options
    sponsorblock_remove: Optional[List[str]] = None # ["sponsor", "intro", "outro", "selfpromo", "preview"]
    
    # Trimming
    clip_start: Optional[str] = None # e.g. "00:01:30"
    clip_end: Optional[str] = None # e.g. "00:02:45"
    
    # Custom yt-dlp extra flags (dict format)
    custom_options: Optional[Dict[str, Any]] = None

class TaskStatus(BaseModel):
    task_id: str
    url: str
    title: Optional[str] = None
    status: str # "queued", "downloading", "processing", "completed", "error"
    progress: float = 0.0 # 0.0 - 100.0
    downloaded_bytes: Optional[int] = 0
    total_bytes: Optional[int] = 0
    speed: Optional[float] = 0.0 # bytes/sec
    speed_str: Optional[str] = ""
    eta: Optional[int] = 0 # seconds
    eta_str: Optional[str] = ""
    filename: Optional[str] = None
    file_path: Optional[str] = None
    error: Optional[str] = None
