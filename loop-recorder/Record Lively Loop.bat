@echo off
title Record Lively Loop
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0loop-tools.ps1" -Mode record -Seconds 20
pause
