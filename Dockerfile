FROM python:3.11-slim

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

# Install ffmpeg, ffprobe, atomicparsley, curl, ca-certificates, and nodejs for JS runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    atomicparsley \
    curl \
    ca-certificates \
    nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy dependency definition
COPY requirements.txt .

# Install python dependencies and ensure yt-dlp is latest
RUN pip install --no-cache-dir -r requirements.txt && \
    pip install --no-cache-dir -U yt-dlp yt-dlp-ejs

# Copy application source (including cookies.txt and templates)
COPY . .

# Create directory for downloads
RUN mkdir -p /app/downloads /app/temp

# Railway exposes $PORT dynamically; default to 8080
EXPOSE 8080

# Start server
CMD ["python3", "main.py"]
