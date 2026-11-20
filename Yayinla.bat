@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Yayinla.ps1" %*
echo.
echo Islem bitti. Kapatmak icin bir tusa bas.
pause >nul
