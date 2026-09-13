@echo off
rem Run script (Windows / cmd). Requires JDK 11+ on the PATH.

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac not found. Install a JDK 11+ and add it to PATH.
    echo         e.g.  winget install Microsoft.OpenJDK.21
    exit /b 1
)

cd /d "%~dp0"
if not exist build mkdir build

echo Compiling...
javac -encoding UTF-8 -d build SnakeGame.java || exit /b 1

echo Starting game...
java -cp build SnakeGame
