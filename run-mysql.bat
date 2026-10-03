@echo off
setlocal

echo =======================================================
echo     Starting PulseRoute MySQL Server
echo =======================================================

set "MYSQLD_BIN=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe"
set "DATA_DIR=%~dp0mysql-data"

if not exist "%MYSQLD_BIN%" (
    echo [ERROR] mysqld.exe not found at %MYSQLD_BIN%
    pause
    exit /b 1
)

echo [INFO] Starting MySQL daemon with data dir: %DATA_DIR%
start "PulseRoute MySQL Server" "%MYSQLD_BIN%" --datadir="%DATA_DIR%" --console

echo [SUCCESS] MySQL Server launched.
