@echo off

set DIR=%~dp0
set NAME=MQTTBroker
set JAR_FILE=${artifactId}-${version}-fat.jar

:: BatchGotAdmin
:-------------------------------------
REM  --> Check for permissions
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"

REM --> If error flag set, we do not have admin.
if '%errorlevel%' NEQ '0' (
    echo Requesting administrative privileges...
    goto UACPrompt
) else ( goto gotAdmin )

:UACPrompt
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    set params = %*:"=""
    echo UAC.ShellExecute "cmd.exe", "/c %~s0 %params%", "", "runas", 1 >> "%temp%\getadmin.vbs"

    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B

:gotAdmin
    pushd "%CD%"
    CD /D "%~dp0"
:--------------------------------------

:parse
GOTO uninstall
REM SHIFT
:endparse
GOTO eof

:install
prunsrv_64bit.exe //IS/%NAME% ^
    --DisplayName="MQTT Broker" ^
    --Description="Starts and manages the MQTT Broker server." ^
    --LogLevel=Debug ^
    --LogPath=%DIR%log ^
    --StartMode=jvm ^
    --Classpath=%DIR%\%JAR_FILE% ^
    --StartClass=Main ^
    --StartParams=run;MQTTBroker;-c;%DIR%config.json ^
    --Startup=manual ^
    --StopMode=jvm ^
    --StopClass=Main ^
    --StopMethod=stop ^
    --StdOutput=auto ^
    --StdError=auto ^
    --StopTimeout=10
GOTO result   

:uninstall
prunsrv_64bit.exe //DS/%NAME%
GOTO result

:start
prunsrv_64bit.exe //ES/%NAME%
GOTO result

:stop
.exe //SS/%NAME%
GOTO result

:status
call:install
prunsrv_64bit.exe //MQ/%NAME%
start prunmgr.exe //ES/%NAME%
GOTO result

:result
if %ERRORLEVEL% GEQ 1 echo Error
if %ERRORLEVEL% EQU 0 echo OK

:eof


    
    
