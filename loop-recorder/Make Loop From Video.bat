@echo off
title Make Loop From Video
if "%~1"=="" (
  echo Drag a video file onto this .bat to turn it into a seamless 1080p loop.
  pause
  exit /b
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0loop-tools.ps1" -Mode file -InputPath "%~1"
pause
