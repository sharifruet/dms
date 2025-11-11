@ECHO off
SETLOCAL

set MAVEN_BATCH_ECHO=off
if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

set MAVEN_OPTS=%MAVEN_OPTS% 

if exist "%HOME%\mavenrc_pre.cmd" call "%HOME%\mavenrc_pre.cmd"

set MAVEN_PROJECTBASEDIR=%~dp0
if not defined MAVEN_PROJECTBASEDIR goto error
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar

if exist "%WRAPPER_JAR%" goto init
if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper...
  powershell -Command "&{ $wc = New-Object System.Net.WebClient; $wc.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%') }"
  if ERRORLEVEL 1 goto error
)

:init
set MAVEN_JAVACONFIG_TYPE=
if exist "%WRAPPER_PROPERTIES%" (
  for /F "tokens=1,2 delims==" %%A in (%WRAPPER_PROPERTIES%) do (
    if "%%A"=="distributionUrl" set WRAPPER_URL=%%B
    if "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
  )
)

set JAVA_EXE=java.exe
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

if exist "%JAVA_EXE%" goto runJava

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo        Please set the JAVA_HOME variable in your environment to match the
echo        location of your Java installation.
goto error

:runJava
%JAVA_EXE% %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error

if exist "%HOME%\mavenrc_post.cmd" call "%HOME%\mavenrc_post.cmd"

ENDLOCAL
goto end

:error
if exist "%HOME%\mavenrc_post.cmd" call "%HOME%\mavenrc_post.cmd"
ENDLOCAL
exit /b 1

:end
exit /b 0

