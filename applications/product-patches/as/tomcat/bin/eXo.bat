@echo off

rem Computes the absolute path of eXo
setlocal ENABLEDELAYEDEXPANSION
for %%i in ( !%~f0! )         do set BIN_DIR=%%~dpi
for %%i in ( !%BIN_DIR%\..! ) do set TOMCAT_HOME=%%~fi

rem Sets some variables
set LOG_OPTS="-Dorg.exoplatform.services.log.Log=org.apache.commons.logging.impl.SimpleLog"
set SECURITY_OPTS="-Djava.security.auth.login.config=%TOMCAT_HOME%\conf\jaas.conf"
set EXO_OPTS=-Dexo.product.developing=true -Dexo.profiles=%1 -Djava.net.preferIPv4Stack=true
set JAVA_OPTS=-Xshare:auto -Xms128m -Xmx512m %LOG_OPTS% %SECURITY_OPTS% %EXO_OPTS%
if ""%1"" == ""ispn"" shift

rem Launches the server
cd %BIN_DIR%
call catalina.bat %1 %2 %3
