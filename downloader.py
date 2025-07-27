from yt_dlp import YoutubeDL
import sys

if len(sys.argv) < 2:
    print("Usage: python downloader.py <YouTube URL>")
    sys.exit(1)

url = sys.argv[1]
options = {
    'format': 'best',
    'outtmpl': 'C:/Users/Krishna/Desktop/%(title)s.%(ext)s',
    'retries': 10,                 # Number of retries for the HTTP connection
    'continuedl': True,            # Continue partial downloads
    'noprogress': False,           # Show progress
    'ignoreerrors': True,          # Continue on download errors
}


with YoutubeDL(options) as ydl:
    ydl.download([url])
