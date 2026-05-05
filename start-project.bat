@echo off
setlocal

cd /d "%~dp0"
title LYGame3 Quick Start

echo.
echo [LYGame3] Checking environment...

where java >nul 2>nul
if errorlevel 1 (
    echo Java not found. Please install Java 17 or later first.
    pause
    exit /b 1
)

if not exist "mvnw.cmd" (
    echo mvnw.cmd not found. Please run this file from the project root.
    pause
    exit /b 1
)

echo Starting Spring Boot server...
start "LYGame3 Server" cmd /k "cd /d ""%~dp0"" && call mvnw.cmd spring-boot:run"

echo Waiting for server startup...
timeout /t 8 /nobreak >nul

echo Opening browser: http://localhost:8080/
start "" http://localhost:8080/

echo Done. You can close this window.
exit /b 0
