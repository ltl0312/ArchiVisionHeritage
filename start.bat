@echo off
title ArchiVision Heritage - Startup

echo.
echo   ================================================
echo     ArchiVision Heritage (ZhiGuan GuJian)
echo     Tech as the vessel, culture as the voyage
echo   ================================================
echo.
echo   Starting all services...
echo.

set "PROJECT_DIR=%~dp0"
set "PYTHON_DIR=D:\Code\Python\vggt-main"

rem ============================================================
rem 1. Python VGGT API (port 8000)
rem ============================================================
echo   [1/3] Starting VGGT Analysis API (port 8000)...
start "ZhiGuan-VGGT-API" cmd /k "cd /d "%PYTHON_DIR%" && echo VGGT Structure Analysis API ^| Port 8000 && echo. && uvicorn api_server:app --host 0.0.0.0 --port 8000 --reload"

rem ============================================================
rem 2. Spring Boot Backend (port 8080)
rem ============================================================
echo   [2/3] Starting Spring Boot Backend (port 8080)...
start "ZhiGuan-Backend" cmd /k "cd /d "%PROJECT_DIR%backend" && echo Spring Boot Backend ^| Port 8080 && echo. && mvn spring-boot:run"

rem ============================================================
rem 3. Vue Frontend (port 5173)
rem ============================================================
echo   [3/3] Starting Vue Frontend (port 5173)...
start "ZhiGuan-Frontend" cmd /k "cd /d "%PROJECT_DIR%frontend" && echo Vue Frontend ^| Port 5173 && echo. && npm run dev"

echo.
echo   ------------------------------------------------
echo   All services launching, please wait...
echo.
echo     VGGT API Docs : http://localhost:8000/docs
echo     Backend API   : http://localhost:8080
echo     Frontend      : http://localhost:5173
echo     Prometheus    : http://localhost:8080/actuator/prometheus
echo.
echo   Run stop.bat to shutdown all services
echo   ------------------------------------------------
echo.
echo   Press any key to close this panel (services will keep running)...
pause >nul
