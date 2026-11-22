import os
import urllib.parse
from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, HTMLResponse
from typing import Dict, List

from models import ExtractRequest, DownloadRequest, VideoMetadata, TaskStatus
import downloader

app = FastAPI(
    title="yt-dlp Railway API Server & Web UI",
    description="Full-featured yt-dlp backend with merging, post-processing, format extraction, web UI, and file delivery",
    version="1.0.0"
)

# Enable CORS for cross-origin or local testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

TEMPLATES_DIR = os.path.join(os.path.dirname(__file__), "templates")
INDEX_HTML = os.path.join(TEMPLATES_DIR, "index.html")

@app.get("/")
def root(request: Request):
    # If browser requests HTML, serve the PC Web interface
    accept = request.headers.get("accept", "")
    if "text/html" in accept and os.path.exists(INDEX_HTML):
        return FileResponse(INDEX_HTML, media_type="text/html")

    return {
        "status": "online",
        "service": "yt-dlp Railway API Server & Web UI",
        "version": "1.0.0",
        "features": [
            "full yt-dlp format selection",
            "video and audio merging (ffmpeg)",
            "post-processing (SponsorBlock, metadata, subtitles, thumbnail embed)",
            "audio extraction (mp3, m4a, flac, opus)",
            "clip trimming",
            "PC Web Interface"
        ]
    }

@app.get("/web", response_class=HTMLResponse)
def web_interface():
    if os.path.exists(INDEX_HTML):
        return FileResponse(INDEX_HTML, media_type="text/html")
    raise HTTPException(status_code=404, detail="Web UI template not found")

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.get("/api/cookies")
def get_cookie_status():
    has_cookies = os.path.exists(downloader.COOKIE_FILE) and os.path.getsize(downloader.COOKIE_FILE) > 10
    return {"has_cookies": has_cookies, "file": downloader.COOKIE_FILE}

@app.post("/api/cookies")
async def update_cookies(request: Request):
    """Save YouTube cookies to bypass cloud datacenter bot challenges."""
    body = await request.body()
    cookie_text = body.decode("utf-8")
    if not cookie_text.strip():
        raise HTTPException(status_code=400, detail="Cookie content is empty")
    with open(downloader.COOKIE_FILE, "w") as f:
        f.write(cookie_text)
    return {"status": "success", "message": "Cookies saved successfully", "bytes": len(cookie_text)}

@app.post("/api/extract", response_model=VideoMetadata)
def extract_metadata(req: ExtractRequest):
    """
    Extracts complete video metadata, formats, and subtitles without downloading.
    """
    if not req.url:
        raise HTTPException(status_code=400, detail="URL cannot be empty")
    try:
        data = downloader.extract_info(req.url)
        return data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to extract media info: {str(e)}")

@app.post("/api/download")
def start_download(req: DownloadRequest):
    """
    Spawns an async download task with all requested yt-dlp and post-processing options.
    """
    if not req.url:
        raise HTTPException(status_code=400, detail="URL cannot be empty")
    
    task_id = downloader.start_download_task(req)
    return {"task_id": task_id, "status": "queued"}

@app.get("/api/tasks/{task_id}", response_model=TaskStatus)
def get_task_status(task_id: str):
    """
    Retrieves real-time progress, speed, ETA, and download status.
    """
    with downloader.tasks_lock:
        task = downloader.tasks.get(task_id)
        if not task:
            raise HTTPException(status_code=404, detail="Task not found")
        return task

@app.get("/api/tasks", response_model=List[TaskStatus])
def list_tasks():
    with downloader.tasks_lock:
        return list(downloader.tasks.values())

@app.get("/api/tasks/{task_id}/file")
def download_task_file(task_id: str):
    """
    Streams the finished file directly to the Android app.
    """
    with downloader.tasks_lock:
        task = downloader.tasks.get(task_id)
        if not task:
            raise HTTPException(status_code=404, detail="Task not found")
        if task.status != "completed" or not task.file_path or not os.path.exists(task.file_path):
            raise HTTPException(status_code=400, detail=f"File not ready or failed. Current status: {task.status}")

        file_path = task.file_path
        filename = task.filename or os.path.basename(file_path)

    quoted_filename = urllib.parse.quote(filename)
    return FileResponse(
        path=file_path,
        media_type="application/octet-stream",
        filename=filename,
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{quoted_filename}"}
    )

@app.delete("/api/tasks/{task_id}")
def delete_task(task_id: str):
    with downloader.tasks_lock:
        task = downloader.tasks.pop(task_id, None)
        if not task:
            raise HTTPException(status_code=404, detail="Task not found")
        
        if task.file_path and os.path.exists(task.file_path):
            try:
                os.remove(task.file_path)
            except Exception:
                pass
        return {"status": "deleted", "task_id": task_id}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    print(f"Starting server on 0.0.0.0:{port}...")
    uvicorn.run("main:app", host="0.0.0.0", port=port)

