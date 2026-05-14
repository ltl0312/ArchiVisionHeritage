@echo off
title ArchiVision Heritage - Shutdown

echo.
echo   ================================================
echo     ArchiVision Heritage - Shutdown All Services
echo   ================================================
echo.

set "killed="

rem ============================================================
rem 1. Kill Python VGGT API (port 8000)
rem ============================================================
echo   [1/3] Stopping VGGT API (port 8000)...
set "pid="
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8000 "') do (
    if not defined pid (
        set "pid=%%a"
        taskkill /PID %%a /F 2>nul
        set "killed=1"
    )
)
if defined pid (echo         Killed PID: %pid%) else (echo         No process found on port 8000)

rem ============================================================
rem 2. Kill Spring Boot Backend (port 8080)
rem ============================================================
echo   [2/3] Stopping Spring Boot Backend (port 8080)...
set "pid="
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8080 "') do (
    if not defined pid (
        set "pid=%%a"
        taskkill /PID %%a /F 2>nul
        set "killed=1"
    )
)
if defined pid (echo         Killed PID: %pid%) else (echo         No process found on port 8080)

rem ============================================================
rem 3. Kill Vue Frontend (port 5173)
rem ============================================================
echo   [3/3] Stopping Vue Frontend (port 5173)...
set "pid="
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173 "') do (
    if not defined pid (
        set "pid=%%a"
        taskkill /PID %%a /F 2>nul
        set "killed=1"
    )
)
if defined pid (echo         Killed PID: %pid%) else (echo         No process found on port 5173)

rem ============================================================
rem 4. Clean up remaining windows
rem ============================================================
echo.
echo   Cleaning up windows...
taskkill /FI "WINDOWTITLE eq ZhiGuan-VGGT-API*" /F 2>nul
taskkill /FI "WINDOWTITLE eq ZhiGuan-Backend*" /F 2>nul
taskkill /FI "WINDOWTITLE eq ZhiGuan-Frontend*" /F 2>nul

echo.
echo   ================================================
echo   All services stopped
echo   ================================================
echo.
echo   Press any key to close...
pause >nul
