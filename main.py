import os
import urllib.parse
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from typing import Dict, List

from models import ExtractRequest, DownloadRequest, VideoMetadata, TaskStatus
import downloader

app = FastAPI(
    title="yt-dlp Railway API Server",
    description="Full-featured yt-dlp backend with merging, post-processing, format extraction, and file delivery for Android",
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

@app.get("/")
def root():
    return {
        "status": "online",
        "service": "yt-dlp Railway API Server",
        "version": "1.0.0",
        "features": [
            "full yt-dlp format selection",
            "video and audio merging (ffmpeg)",
            "post-processing (SponsorBlock, metadata, subtitles, thumbnail embed)",
            "audio extraction (mp3, m4a, flac, opus)",
            "clip trimming"
        ]
    }

@app.get("/health")
def health_check():
    return {"status": "healthy"}

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

