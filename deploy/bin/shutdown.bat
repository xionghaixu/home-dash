@echo off
setlocal

for /f "tokens=2" %%a in ('tasklist ^| findstr "java"') do (
    set "pid=%%a"
)

if not defined pid (
    echo no home-dash server running...
    exit /b 1
)

echo kill home-dash server...
taskkill /PID %pid% /F
echo success kill server at pid:%pid%
