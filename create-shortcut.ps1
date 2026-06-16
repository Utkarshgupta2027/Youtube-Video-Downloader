# Create Desktop Shortcut for YouTube Video Downloader
# This script creates a shortcut on the desktop

$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$runVbsPath = Join-Path $projectPath "run.vbs"
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath "YouTube Downloader.lnk"

# Create the WScript.Shell COM object
$shell = New-Object -ComObject WScript.Shell

# Create the shortcut
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $runVbsPath
$shortcut.WorkingDirectory = $projectPath
$shortcut.Description = "YouTube Video Downloader - Download videos in your preferred quality"
$shortcut.IconLocation = (Join-Path $projectPath "yt_icon.ico") + ", 0"

# Save the shortcut
$shortcut.Save()

Write-Host "Desktop shortcut created successfully!" -ForegroundColor Green
Write-Host "Shortcut location: $shortcutPath"
Write-Host ""
Write-Host "You can now double-click the 'YouTube Downloader' icon on your desktop to launch the application."
