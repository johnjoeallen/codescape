@echo off
setlocal enabledelayedexpansion

set "DIST_DIR=%~dp0.."
cd /d "%DIST_DIR%"

set "JAR="
for %%f in ("%DIST_DIR%\lib\codescape-service-*.jar") do set "JAR=%%f"

if "%JAR%"=="" (
  echo Could not find codescape-service jar under %DIST_DIR%\lib
  exit /b 1
)

rem Java selection, in priority order:
rem   1. --java-home <path> given on this invocation
rem   2. .java-home marker file (saved by a prior --java-home that
rem      validated OK)
rem   3. JAVA_HOME environment variable
rem   4. `java` on PATH
rem   5. the bundled runtime under runtime\, as a last resort (with a
rem      warning)
rem
rem A compliant system Java is preferred over the bundled one because
rem some corporate endpoint security software (e.g. Carbon Black) blocks
rem execution of unsigned/bundled binaries -- the runtime\ JVM we ship
rem may simply not be runnable in a locked-down environment even though
rem it's present and otherwise correct. --java-home/.java-home exist so
rem a user can point at a compliant install without editing their
rem existing JAVA_HOME, which may be relied on by other tools.

rem --- parse --java-home / --java-home=<path>, keep everything else ---
set "JAVA_HOME_FLAG="
set "REMAINING_ARGS="

:parse_args
if "%~1"=="" goto after_parse
if /i "%~1"=="--java-home" (
  set "JAVA_HOME_FLAG=%~2"
  shift
  shift
  goto parse_args
)
set "ARG=%~1"
if /i "!ARG:~0,12!"=="--java-home=" (
  set "JAVA_HOME_FLAG=!ARG:~12!"
  shift
  goto parse_args
)
set "REMAINING_ARGS=!REMAINING_ARGS! "%~1""
shift
goto parse_args
:after_parse

set "JAVA_HOME_FILE=%DIST_DIR%\.java-home"
set "JAVA_BIN="

if defined JAVA_HOME_FLAG (
  call :check_java_home_dir "%JAVA_HOME_FLAG%"
  if defined JAVA_HOME_BIN (
    set "JAVA_BIN=!JAVA_HOME_BIN!"
    (echo !JAVA_HOME_FLAG!) > "%JAVA_HOME_FILE%"
    echo codescape: using Java 25+ at '%JAVA_HOME_FLAG%' ^(saved to %JAVA_HOME_FILE% -- will be used automatically next time, no need to pass --java-home again^) 1>&2
  ) else (
    echo codescape: --java-home '%JAVA_HOME_FLAG%' doesn't look like a Java 25+ install; ignoring it and checking other options. 1>&2
  )
)

if not defined JAVA_BIN (
  if exist "%JAVA_HOME_FILE%" (
    set "SAVED_JAVA_HOME="
    set /p SAVED_JAVA_HOME=<"%JAVA_HOME_FILE%"
    if defined SAVED_JAVA_HOME (
      call :check_java_home_dir "!SAVED_JAVA_HOME!"
      if defined JAVA_HOME_BIN set "JAVA_BIN=!JAVA_HOME_BIN!"
    )
  )
)

if not defined JAVA_BIN (
  if defined JAVA_HOME (
    call :check_java_home_dir "%JAVA_HOME%"
    if defined JAVA_HOME_BIN set "JAVA_BIN=!JAVA_HOME_BIN!"
  )
)

if not defined JAVA_BIN (
  where java >nul 2>&1
  if !ERRORLEVEL! EQU 0 (
    call :check_java_version java
    if "!VERSION_OK!"=="1" set "JAVA_BIN=java"
  )
)

if not defined JAVA_BIN (
  if exist "%DIST_DIR%\runtime\bin\java.exe" (
    echo codescape: no system Java 25+ found via --java-home, .java-home, JAVA_HOME, or PATH; falling back to the bundled runtime under runtime\. If your environment blocks running bundled or unsigned executables, such as Carbon Black or similar endpoint security software, install Java 25+ and pass --java-home ^<path^>, or set JAVA_HOME/PATH, instead. 1>&2
    set "JAVA_BIN=%DIST_DIR%\runtime\bin\java.exe"
  ) else (
    set "JAVA_BIN=java"
  )
)

"!JAVA_BIN!" -jar "%JAR%" --spring.config.additional-location="file:%DIST_DIR%\config\\" !REMAINING_ARGS!
exit /b %ERRORLEVEL%

:check_java_version
rem %1 = java binary to check (path or bare command name). Sets
rem VERSION_OK=1 if it reports major version 25+, else VERSION_OK=0.
set "VERSION_OK=0"
set "RAWVER="
set "V_MAJOR="
for /f "tokens=3" %%v in ('"%~1" -version 2^>^&1 ^| findstr /i "version"') do set "RAWVER=%%v"
if defined RAWVER (
  set "RAWVER=!RAWVER:"=!"
  for /f "delims=. tokens=1" %%m in ("!RAWVER!") do set "V_MAJOR=%%m"
)
if defined V_MAJOR if !V_MAJOR! GEQ 25 set "VERSION_OK=1"
goto :eof

:check_java_home_dir
rem %1 = a JAVA_HOME-style directory. Sets JAVA_HOME_BIN to a usable
rem java.exe under it if found and version 25+, else clears JAVA_HOME_BIN.
set "JAVA_HOME_BIN="
if exist "%~1\bin\java.exe" (
  call :check_java_version "%~1\bin\java.exe"
  if "!VERSION_OK!"=="1" set "JAVA_HOME_BIN=%~1\bin\java.exe"
)
goto :eof
