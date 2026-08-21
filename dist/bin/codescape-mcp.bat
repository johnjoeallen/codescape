@echo off
setlocal

set "DIST_DIR=%~dp0.."
set "JAR="
for %%f in ("%DIST_DIR%\lib\codescape-mcp-*.jar") do set "JAR=%%f"

if "%JAR%"=="" (
  echo Could not find codescape-mcp jar under %DIST_DIR%\lib
  exit /b 1
)

set "JAVA_BIN=java"
if exist "%DIST_DIR%\runtime\bin\java.exe" set "JAVA_BIN=%DIST_DIR%\runtime\bin\java.exe"

"%JAVA_BIN%" -jar "%JAR%" %*
