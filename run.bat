@echo off
REM YouTube Video Downloader - GUI Launcher
REM This script compiles and runs the Java GUI application

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Change to the script directory
cd /d "%SCRIPT_DIR%"

REM Check if Python is installed (required for downloader.py)
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

REM Check if Java is installed
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Java Development Kit (JDK) is not installed or not in PATH
    echo Please install Java JDK 11+ from https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

REM Check if requirements are installed
echo Checking Python dependencies...
pip show yt-dlp >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing required Python packages...
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        echo.
        echo ERROR: Failed to install Python dependencies
        echo.
        pause
        exit /b 1
    )
)

REM Compile Java files if needed
echo Compiling Java application...
if not exist "YouTubeDownloaderGUI.class" (
    javac YouTubeDownloaderGUI.java Awtex.java 2>nul
)

REM Run the GUI application
echo Starting YouTube Video Downloader...
java YouTubeDownloaderGUI

REM Pause on error for debugging
if %errorlevel% neq 0 (
    echo.
    echo An error occurred while running the application.
    pause
)

endlocal
