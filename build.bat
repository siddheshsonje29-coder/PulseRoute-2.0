@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo   PulseRoute Java Servlet Authentication Build Tool
echo =======================================================

:: 1. Detect Tomcat installation
set TOMCAT_HOME=C:\apache-tomcat-11.0.4
if not exist "%TOMCAT_HOME%" (
    if exist "C:\apache-tomcat-9.0.100" (
        set TOMCAT_HOME=C:\apache-tomcat-9.0.100
    ) else (
        echo [ERROR] Tomcat not found in C:\apache-tomcat-11.0.4 or C:\apache-tomcat-9.0.100.
        echo Please set TOMCAT_HOME to your Tomcat installation path.
        exit /b 1
    )
)

echo [INFO] Using Tomcat at: %TOMCAT_HOME%
set SERVLET_JAR=%TOMCAT_HOME%\lib\servlet-api.jar

if not exist "%SERVLET_JAR%" (
    echo [ERROR] servlet-api.jar not found in %TOMCAT_HOME%\lib
    exit /b 1
)

:: 2. Detect JDK bin for javac and jar
set "JAR_CMD=jar"
where jar >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Program Files\Java\jdk-26.0.1\bin\jar.exe" (
        set "JAR_CMD=C:\Program Files\Java\jdk-26.0.1\bin\jar.exe"
    ) else (
        for /d %%D in ("C:\Program Files\Java\jdk*") do (
            if exist "%%D\bin\jar.exe" set "JAR_CMD=%%D\bin\jar.exe"
        )
    )
)

:: 3. Prepare classes directory
echo [INFO] Preparing WEB-INF\classes directory...
if not exist "src\main\webapp\WEB-INF\classes" (
    mkdir "src\main\webapp\WEB-INF\classes"
)

:: 4. Collect Java source files
echo [INFO] Finding Java source files...
set SOURCES=
for /R "src\main\java" %%F in (*.java) do (
    set SOURCES=!SOURCES! "%%F"
)

:: 5. Compile Java sources
echo [INFO] Compiling Java classes with javac...
javac -encoding UTF-8 -d "src\main\webapp\WEB-INF\classes" -cp "%SERVLET_JAR%" !SOURCES!

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Java compilation completed successfully!

:: 6. Copy resources
if exist "src\main\resources\db.properties" (
    copy /Y "src\main\resources\db.properties" "src\main\webapp\WEB-INF\classes\db.properties" >nul
)

:: 7. Package into WAR file
echo [INFO] Packaging pulseroute.war...
pushd "%~dp0src\main\webapp"
"%JAR_CMD%" -cvf "%~dp0pulseroute.war" * >nul
popd

if exist "%~dp0pulseroute.war" (
    echo [SUCCESS] Built pulseroute.war successfully!
    copy /Y "%~dp0pulseroute.war" "%TOMCAT_HOME%\webapps\pulseroute.war" >nul
    echo [SUCCESS] Automatically deployed pulseroute.war to %TOMCAT_HOME%\webapps\
) else (
    echo [ERROR] WAR creation failed.
    exit /b 1
)

echo =======================================================
echo   Build Finished Successfully
echo =======================================================
