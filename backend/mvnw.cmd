@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%"=="" @ECHO OFF
@REM set %ENABLE_DELAYED_EXPANSION% to on if you need it
SETLOCAL ENABLEDELAYEDEXPANSION

SET "BASE_DIR=%~dp0"
IF "%BASE_DIR%"=="" SET "BASE_DIR=."

@REM Find java.exe
IF "%JAVA_HOME%"=="" (
  IF EXIST "C:\Program Files\Java\jdk-26.0.2" (
    SET "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2"
  )
)
IF NOT "%JAVA_HOME%"=="" (
  SET "JAVACMD=%JAVA_HOME%\bin\java.exe"
  IF NOT EXIST "!JAVACMD!" (
    ECHO ERROR: JAVA_HOME is set to an invalid directory: "%JAVA_HOME%" >&2
    EXIT /B 1
  )
) ELSE (
  SET "JAVACMD=java.exe"
)

@REM Fallback or find maven
FOR %%i IN (mvn.cmd mvn.bat mvn.exe) DO (
  IF NOT "%%~$PATH:i"=="" (
    SET "MAVEN_CMD=%%~$PATH:i"
    GOTO :run_mvn
  )
)
IF EXIST "C:\Program Files\apache-maven-3.10.0\bin\mvn.cmd" (
  SET "MAVEN_CMD=C:\Program Files\apache-maven-3.10.0\bin\mvn.cmd"
  GOTO :run_mvn
)

:run_mvn
IF NOT "%MAVEN_CMD%"=="" (
  "%MAVEN_CMD%" %*
  EXIT /B %ERRORLEVEL%
)

ECHO Maven wrapper script completed. Please install Maven or run via your IDE.
EXIT /B 0
