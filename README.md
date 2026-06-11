Run the GUI (Java) and downloader (Python)

Prerequisites:
- Java JDK (11+)
- Python 3 and `pip install -r requirements.txt`
- (Optional) `ffmpeg` on PATH for some formats

To run the GUI from source:

```bash
# in project root
javac YouTubeDownloaderGUI.java Awtex.java JavaGUICompleteDemo.java
java YouTubeDownloaderGUI
```

To run the downloader manually:

```bash
python downloader.py <YouTube URL>
```

The Java GUI calls `downloader.py` and expects progress lines like `12.34%` to update the progress bar. The downloader saves files under `downloads/` next to the script.
