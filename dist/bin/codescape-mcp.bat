@echo off
setlocal enabledelayedexpansion

set "DIST_DIR=%~dp0.."
set "JAR="
for %%f in ("%DIST_DIR%\lib\codescape-mcp-*.jar") do set "JAR=%%f"

if "%JAR%"=="" (
  echo Could not find codescape-mcp jar under %DIST_DIR%\lib
  exit /b 1
)

rem Prefer a system Java 25+ over the bundled runtime: some corporate
rem endpoint security tools (e.g. Carbon Black) block execution of
rem unsigned/bundled binaries, so the runtime\ JVM we ship may simply not
rem be runnable in a locked-down environment even though it's present and
rem otherwise correct. Only fall back to the bundled runtime -- with a
rem warning -- when no suitable system Java is found.
set "USE_SYSTEM_JAVA=0"
where java >nul 2>&1
if !ERRORLEVEL! EQU 0 (
  set "SYSTEM_JAVA_MAJOR="
  for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "RAWVER=%%v"
  set "RAWVER=!RAWVER:"=!"
  for /f "delims=. tokens=1" %%m in ("!RAWVER!") do set "SYSTEM_JAVA_MAJOR=%%m"
  if defined SYSTEM_JAVA_MAJOR (
    if !SYSTEM_JAVA_MAJOR! GEQ 25 set "USE_SYSTEM_JAVA=1"
  )
)

if "!USE_SYSTEM_JAVA!"=="1" (
  set "JAVA_BIN=java"
) else if exist "%DIST_DIR%\runtime\bin\java.exe" (
  echo codescape: no system Java 25+ found on PATH; falling back to the bundled runtime under runtime\. If your environment blocks running bundled or unsigned executables, such as Carbon Black or similar endpoint security software, install Java 25+ and make sure java resolves to it on PATH instead. 1>&2
  set "JAVA_BIN=%DIST_DIR%\runtime\bin\java.exe"
) else (
  set "JAVA_BIN=java"
)

"!JAVA_BIN!" -jar "%JAR%" %*
