from yt_dlp import YoutubeDL
import sys
import os

if len(sys.argv) < 2:
    print("Usage: python downloader.py <YouTube URL>")
    sys.exit(1)

url = sys.argv[1]

# Ensure downloads directory exists next to the script
downloads_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "downloads")
os.makedirs(downloads_dir, exist_ok=True)

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

options = {
    'format': 'best',
    'outtmpl': os.path.join(downloads_dir, '%(title)s.%(ext)s'),
    'retries': 10,
    'continuedl': True,
    'noprogress': False,
    'ignoreerrors': True,
    'progress_hooks': [progress_hook],
}

with YoutubeDL(options) as ydl:
    ydl.download([url])
