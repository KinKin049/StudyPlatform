@echo off
setlocal
cd /d "%~dp0.."

echo Starting StudyPlatform backend and frontend...
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173

start "StudyPlatform Backend" powershell -NoExit -ExecutionPolicy Bypass -File "%cd%\scripts\start-backend.ps1"
start "StudyPlatform Frontend" powershell -NoExit -ExecutionPolicy Bypass -File "%cd%\scripts\start-frontend.ps1"

endlocal
