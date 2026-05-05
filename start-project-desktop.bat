@echo off
setlocal

set "PROJECT_DIR="

if exist "%~dp0mvnw.cmd" if exist "%~dp0pom.xml" (
    set "PROJECT_DIR=%~dp0"
    goto :found
)

for /f "delims=" %%I in ('dir /s /b /ad "%~dp0god" 2^>nul') do (
    if exist "%%~fI\mvnw.cmd" if exist "%%~fI\pom.xml" (
        set "PROJECT_DIR=%%~fI"
        goto :found
    )
)

:found
if not defined PROJECT_DIR (
    echo Project folder not found.
    echo Put this bat on your Desktop, or keep it in the project root.
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%"
title LYGame3 Desktop Start

echo.
echo [LYGame3] Starting project from:
echo %PROJECT_DIR%

where java >nul 2>nul
if errorlevel 1 (
    echo Java not found. Please install Java 17 or later first.
    pause
    exit /b 1
)

if not exist "mvnw.cmd" (
    echo mvnw.cmd not found in project folder.
    pause
    exit /b 1
)

echo Starting Spring Boot server...
start "LYGame3 Server" cmd /k "cd /d ""%PROJECT_DIR%"" && call mvnw.cmd spring-boot:run"

echo Waiting for server startup...
timeout /t 8 /nobreak >nul

echo Opening browser: http://localhost:8080/
start "" http://localhost:8080/

echo Done. You can close this window.
exit /b 0
