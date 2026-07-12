@echo off
setlocal
where pwsh >nul 2>nul
if %ERRORLEVEL% NEQ 0 goto windows_powershell
pwsh -NoProfile -File "%~dp0build.ps1" %*
exit /b %ERRORLEVEL%

:windows_powershell
where powershell >nul 2>nul
if %ERRORLEVEL% NEQ 0 goto missing_powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
exit /b %ERRORLEVEL%

:missing_powershell
echo PowerShell 7 or Windows PowerShell is required. 1>&2
exit /b 1
