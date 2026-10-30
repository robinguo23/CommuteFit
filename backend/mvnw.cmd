@echo off
setlocal
set WRAPPER_DIR=%~dp0
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.10.0\commute-fit\apache-maven-3.10.0
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  call "%MAVEN_HOME%\bin\mvn.cmd" %*
  exit /b %ERRORLEVEL%
)
set DIST_ROOT=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.10.0\commute-fit
set DIST_ZIP=%TEMP%\apache-maven-3.10.0-commute-fit.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; New-Item -ItemType Directory -Force -Path '%DIST_ROOT%' | Out-Null; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.10.0/apache-maven-3.10.0-bin.zip' -OutFile '%DIST_ZIP%'; Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%DIST_ROOT%' -Force"
if errorlevel 1 exit /b %ERRORLEVEL%
del "%DIST_ZIP%"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
