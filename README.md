# yt-dlp Railway API Server

A high-performance FastAPI server running full-featured **yt-dlp** with **FFmpeg** and **AtomicParsley**. Designed for deployment to **Railway** as the backend engine for Android, iOS, or web clients.

---

## 🚀 Features

- **Full Format Selection**: Supports all yt-dlp format strings (e.g. `bestvideo+bestaudio/best`, `bv*[ext=mp4]+ba[ext=m4a]`, specific resolution / codec tags).
- **FFmpeg Merging**: Automatically merges high-res video (1080p, 1440p, 4K, 8K, 60fps, HDR) with uncompressed/best audio into MP4/MKV.
- **Audio Extraction**: Extracts audio and converts to MP3, M4A, FLAC, OPUS, or WAV with quality selection (320kbps, 256kbps, etc.).
- **Post-Processing Capabilities**:
  - **SponsorBlock**: Automatically detect and remove sponsored segments, intros, outros, self-promo.
  - **Embed Thumbnails**: Write cover art directly into the MP4/MKV/MP3 container.
  - **Embed Subtitles**: Fetch auto-generated or official subtitles and embed into video streams.
  - **Embed Metadata & Chapters**: Tag artists, album, title, and video chapters.
  - **Clip Trimming**: Download only specific start/end timestamps without downloading the full video.
- **Real-Time Progress Tracking**: Reports download percentage, speed (MB/s), ETA (seconds), and post-processing stages.
- **Automated Disk Cleanup**: Background worker cleans up completed downloads older than 2 hours to prevent disk saturation.

---

## ⚡ Deployment to Railway (Step-by-Step)

### Option 1: Deploy from GitHub (Recommended)

1. Create a new GitHub repository (e.g. `yt-dlp-railway-server`) and push the contents of the `server/` directory.
2. Go to [railway.com](https://railway.com) (or [railway.app](https://railway.app)) and log in.
3. Click **"New Project"** -> **"Deploy from GitHub repo"**.
4. Select your newly created repository.
5. Railway will automatically detect the `Dockerfile` and build the container with Python 3.11, FFmpeg, and yt-dlp!
6. Once deployed:
   - Click on your service -> **Settings** -> **Networking** -> **Generate Domain**.
   - You will get a public URL like: `https://yt-dlp-server-production.up.railway.app`.
   - Copy this URL into your Android app settings!

### Option 2: Deploy via Railway CLI

```bash
# 1. Install Railway CLI
npm i -g @railway/cli

# 2. Login to Railway
railway login

# 3. Inside the server/ directory:
cd server
railway init
railway up
```

---

## 📡 API Endpoints

### 1. Extract Video Metadata & Formats
- **`POST /api/extract`**
```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```
**Response**:
Returns title, channel, duration, thumbnail, and the full array of formats (resolution, fps, codecs, approximate file sizes) and available subtitles.

---

### 2. Start Download Task with Merging & Post-Processing
- **`POST /api/download`**
```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "format_selector": "bestvideo*+bestaudio/best",
  "audio_only": false,
  "merge_output_format": "mp4",
  "embed_thumbnail": true,
  "embed_subtitles": false,
  "embed_metadata": true,
  "sponsorblock_remove": ["sponsor", "intro", "outro"],
  "clip_start": null,
  "clip_end": null
}
```
**Response**:
```json
{
  "task_id": "a1b2c3d4",
  "status": "queued"
}
```

---

### 3. Check Task Status & Real-time Progress
- **`GET /api/tasks/{task_id}`**
**Response**:
```json
{
  "task_id": "a1b2c3d4",
  "status": "downloading",
  "progress": 64.5,
  "downloaded_bytes": 10485760,
  "total_bytes": 16252928,
  "speed_str": "4.20 MB/s",
  "eta_str": "1s",
  "filename": "a1b2c3d4_Video Title.mp4"
}
```

---

### 4. Download Completed File
- **`GET /api/tasks/{task_id}/file`**
Streams the merged / post-processed file with proper `Content-Disposition` header directly to the Android device.
