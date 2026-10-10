@echo off
rem Double-click friendly wrapper for bootstrap.ps1 (prefers PowerShell 7, falls back to Windows PowerShell).
where pwsh >nul 2>nul
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap.ps1" %*
)
if "%~1"=="" pause
