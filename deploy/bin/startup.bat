@echo off
setlocal

set "HomeDash_HOME=%~dp0.."

set "JAVA_OPTS=%JAVA_OPTS% -DHomeDash.homeDir=%HomeDash_HOME%"
set "JAVA_OPTS=%JAVA_OPTS% -jar %HomeDash_HOME%\lib\home-dash.jar"
set "JAVA_OPTS=%JAVA_OPTS% --spring.config.location=%HomeDash_HOME%\conf\application.yml"

if not exist "%HomeDash_HOME%\data\logs" mkdir "%HomeDash_HOME%\data\logs"

start "home-dash" java %JAVA_OPTS%
echo started home-dash!!!
