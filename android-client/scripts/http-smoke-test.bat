@echo off
REM Runs scripts\http-smoke-test.ps1. Optional argument: base URL (default http://localhost:8080/scasp)
if "%~1"=="" (powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0http-smoke-test.ps1") else (powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0http-smoke-test.ps1" -Base "%~1")
pause
