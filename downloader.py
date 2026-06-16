from yt_dlp import YoutubeDL
import sys
import os
import re

YOUTUBE_URL_PATTERN = re.compile(
    r'^(https?://)?(www\.|m\.)?(youtube\.com|youtu\.be|youtube-nocookie\.com)/'
    r'(watch\?v=[^&\s]+|embed/[^\s]+|shorts/[^\s]+|playlist\?list=[^&\s]+|[^\s]+)$',
    re.IGNORECASE
)

if len(sys.argv) < 2:
    print("Usage: python downloader.py <YouTube URL> [format]")
    print("Formats: best, worst, video-only, audio-only, 1080p, 720p, 480p, 360p, 240p, 144p")
    sys.exit(1)

url = sys.argv[1]
format_choice = sys.argv[2].lower() if len(sys.argv) > 2 else "best"

if not YOUTUBE_URL_PATTERN.match(url.strip()):
    print("Error: Invalid YouTube URL.")
    sys.exit(1)

# Ensure downloads directory exists next to the script
downloads_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "downloads")
os.makedirs(downloads_dir, exist_ok=True)

def get_format_string(quality):
    """
    Map quality choice to yt_dlp format string.
    
    Formats:
    - best: Best quality (video + audio)
    - worst: Worst quality (smallest file size)
    - video-only: Best video without audio
    - audio-only: Best audio only (mp3/m4a)
    - 1080p, 720p, 480p, etc: Specific resolution with audio
    """
    format_map = {
        "best": "best",  # Automatically selects best video+audio
        "worst": "worst",  # Worst quality available
        "video-only": "bestvideo",  # Best video without audio
        "audio-only": "bestaudio",  # Best audio only
        "1080p": "bestvideo[height=1080]+bestaudio/best[height=1080]/best",
        "720p": "bestvideo[height=720]+bestaudio/best[height=720]/best",
        "480p": "bestvideo[height=480]+bestaudio/best[height=480]/best",
        "360p": "bestvideo[height=360]+bestaudio/best[height=360]/best",
        "240p": "bestvideo[height=240]+bestaudio/best[height=240]/best",
        "144p": "bestvideo[height=144]+bestaudio/best[height=144]/best",
    }
    return format_map.get(quality, "best")


def progress_hook(d):
    # d is a dict with keys like 'status', 'downloaded_bytes', 'total_bytes'
    if d.get('status') == 'downloading':
        downloaded = d.get('downloaded_bytes') or 0
        total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
        if total:
            percent = downloaded / total * 100.0
            # Print a simple percentage line that Java GUI will parse (e.g. "12.34%")
            print(f"{percent:.2f}%")
        else:
            # Unknown total size; print a marker
            print("0%")
    elif d.get('status') == 'finished':
        print("100%")


# Select format based on user choice
selected_format = get_format_string(format_choice)

options = {
    'format': selected_format,
    'outtmpl': os.path.join(downloads_dir, '%(title)s.%(ext)s'),
    'retries': 10,
    'continuedl': True,
    'noprogress': False,
    'ignoreerrors': True,
    'progress_hooks': [progress_hook],
}

# For audio-only, post-process to mp3
if format_choice == "audio-only":
    options['postprocessors'] = [{
        'key': 'FFmpegExtractAudio',
        'preferredcodec': 'mp3',
        'preferredquality': '192',
    }]

with YoutubeDL(options) as ydl:
    ydl.download([url])
