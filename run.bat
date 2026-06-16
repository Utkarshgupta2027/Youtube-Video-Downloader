@echo off
setlocal

REM Move to project folder
cd /d "%~dp0"

echo Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo Python is not installed or not in PATH.
    pause
    exit /b 1
)

echo Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo Java is not installed or not in PATH.
    pause
    exit /b 1
)

echo Checking yt-dlp...
pip show yt-dlp >nul 2>&1
if errorlevel 1 (
    echo Installing Python dependencies...
    pip install -r requirements.txt
)

echo Checking Java classes...
if not exist "YouTubeDownloaderGUI.class" (
    echo Compiling Java files...
    javac YouTubeDownloaderGUI.java Awtex.java
    if errorlevel 1 (
        echo Java compilation failed.
        pause
        exit /b 1
    )
)

echo Starting application...
java YouTubeDownloaderGUI

if errorlevel 1 (
    echo.
    echo Application exited with an error.
    pause
)

endlocal