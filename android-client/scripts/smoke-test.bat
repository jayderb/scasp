@echo off
REM Double-click or run from cmd: runs scripts\smoke-test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0smoke-test.ps1"
pause
