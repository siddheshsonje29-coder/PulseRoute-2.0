@echo off
setlocal

echo =======================================================
echo     Starting PulseRoute on Apache Tomcat
echo =======================================================

:: Set JAVA_HOME if not already in system environment
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
)

:: Set CATALINA_HOME
if "%CATALINA_HOME%"=="" (
    set "CATALINA_HOME=C:\apache-tomcat-11.0.4"
)

echo [INFO] JAVA_HOME     = %JAVA_HOME%
echo [INFO] CATALINA_HOME = %CATALINA_HOME%

if not exist "%CATALINA_HOME%\bin\startup.bat" (
    echo [ERROR] Tomcat startup.bat not found in %CATALINA_HOME%\bin
    pause
    exit /b 1
)

:: Rebuild WAR if needed
if not exist "%CATALINA_HOME%\webapps\pulseroute.war" (
    echo [INFO] Copying pulseroute.war to Tomcat webapps...
    call "%~dp0build.bat"
    copy /Y "%~dp0pulseroute.war" "%CATALINA_HOME%\webapps\pulseroute.war"
)

echo [INFO] Launching Tomcat...
echo [INFO] Once started, open your browser at:
echo         http://localhost:8080/pulseroute/
echo.

call "%CATALINA_HOME%\bin\startup.bat"

echo [SUCCESS] Tomcat startup command issued.
