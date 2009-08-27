@Echo Off
Rem ---------------------------------------------------------------------------
Rem JOnAS: Java(TM) Open Application Server
Rem Copyright (C) 1999-2005 Bull S.A.
Rem Contact: jonas-team@objectweb.org
Rem
Rem This library is free software; you can redistribute it and/or
Rem modify it under the terms of the GNU Lesser General Public
Rem License as published by the Free Software Foundation; either
Rem version 2.1 of the License, or any later version.
Rem
Rem This library is distributed in the hope that it will be useful,
Rem but WITHOUT ANY WARRANTY; without even the implied warranty of
Rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Rem Lesser General Public License for more details.
Rem
Rem You should have received a copy of the GNU Lesser General Public
Rem License along with this library; if not, write to the Free Software
Rem Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
Rem USA
Rem
Rem Initial developer(s): Philippe Durieux
Rem Contributor(s): Miroslav Halas
Rem        Adriana Danes :
Rem          - Change /config en /conf
Rem          - Use JONAS_BASE
Rem        Jerome Pioux:
Rem        - Remove extra spaces in :start_jonas_bg after %JAVA% - was causing
Rem          bug #306113 - happen only if the script is used from the tarball
Rem          (UNIX format [LF]). As a side note, with 4.8.3, the script will
Rem          be saved as DOS format [CRLF] inside SVN.
Rem ---------------------------------------------------------------------------
Rem $Id: jonas.bat 9856 2006-11-21 13:32:40 +0000 (Tue, 21 Nov 2006) durieuxp $
Rem ---------------------------------------------------------------------------

Rem Keep variables local to this script
setlocal ENABLEDELAYEDEXPANSION

Rem ---------------------------------------------
Rem set JONAS_ROOT
Rem ---------------------------------------------
Rem %~f0 is the script path
@set this_fqn=%~f0
@for %%i in ( !this_fqn! ) do @set bin_nt_dir=%%~dpi
@for %%i in ( !bin_nt_dir!\.. ) do @set bin_dir=%%~dpi
@for %%i in ( !bin_dir! ) do @set JONAS_ROOT=%%~dpi

Rem ---------------------------------------------
Rem set environment
Rem ---------------------------------------------
if [%JONAS_ROOT%]==[] goto setroot
call %JONAS_ROOT%\bin\nt\setenv.bat
call %JONAS_ROOT%\bin\nt\config_env.bat
set JONAS_LIB=%JONAS_ROOT%\lib

Rem include jonas classes
Set CLASSPATH=%JONAS_ROOT%\lib\common\ow_jonas_bootstrap.jar;%JONAS_ROOT%\lib\commons\jonas\jakarta-commons\commons-logging-api.jar;%CLASSPATH%


Rem ---------------------------------------------
Rem set JAVA_OPTS
Rem ---------------------------------------------
Rem JAVA_HOME must be  set since config_env.bat requires it for tools.jar
if ["%JAVA_HOME%"]==[""] goto setjava

Rem JONAS_OPTS may be already partially initialized
set JONAS_OPTS=%JONAS_OPTS% -Dinstall.root=%JONAS_ROOT%
set JONAS_OPTS=%JONAS_OPTS% -Djonas.base=%JONAS_BASE%
set JONAS_OPTS=%JONAS_OPTS% -Djava.security.policy=%JONAS_ROOT%\conf\java.policy
set JONAS_OPTS=%JONAS_OPTS% -Djonas.classpath=%XTRA_CLASSPATH%
set JONAS_OPTS=%JONAS_OPTS% -Djonas.default.classloader=true
set JONAS_OPTS=%JONAS_OPTS% -Dorg.omg.CORBA.ORBClass=org.jacorb.orb.ORB
set JONAS_OPTS=%JONAS_OPTS% -Dorg.omg.CORBA.ORBSingletonClass=org.jacorb.orb.ORBSingleton
set JONAS_OPTS=%JONAS_OPTS% -Dorg.omg.PortableInterceptor.ORBInitializerClass.standard_init=org.jacorb.orb.standardInterceptors.IORInterceptorInitializer
set JONAS_OPTS=%JONAS_OPTS% -Djavax.rmi.CORBA.PortableRemoteObjectClass=org.objectweb.carol.rmi.multi.MultiPRODelegate
set JONAS_OPTS=%JONAS_OPTS% -Djava.naming.factory.initial=org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory
set JONAS_OPTS=%JONAS_OPTS% -Djavax.rmi.CORBA.UtilClass=org.objectweb.carol.util.delegate.UtilDelegateImpl
set JONAS_OPTS=%JONAS_OPTS% -Djava.security.auth.login.config=%JONAS_BASE%\conf\jaas.config
set JONAS_OPTS=%JONAS_OPTS% -Djava.endorsed.dirs=%JONAS_ROOT%\lib\endorsed

Rem ----------------------- Begin eXo configuration ----------------------------
cd %JONAS_ROOT%/bin
set JONAS_OPTS=%JONAS_OPTS% -Dorg.exoplatform.services.log.Log=org.apache.commons.logging.impl.SimpleLog -Djava.awt.headless=true
set JAVA_OPTS=%JAVA_OPTS% -Xmx512M
Rem ------------------------ End eXo configuration -----------------------------

Rem ---------------------------------------------
Rem Get args
Rem ---------------------------------------------

if [%1]==[]   goto no_arg

set ARGS=
set MODE=
set JONASNAME=
set PINGTIMEOUT=
set NAME_OPT=
set PINGTIMEOUT_OPT=
set ANNOTATE=
set WHERE=background

:loop_on_args
Set VALUE=%~1
if [%1]==[start]     goto start_arg
if [%1]==[stop]      goto stop_arg
if [%1]==[admin]     goto admin_arg
if [%1]==[version]   goto version_arg
if [%1]==[check]     goto check_arg
if [%1]==[ntservice] goto ntservice_arg
if [%1]==[-fg]       goto fg_arg
if [%1]==[-bg]       goto bg_arg
if [%1]==[-win]      goto win_arg
if [%1]==[-n]        goto n_arg
if [%1]==[-timeout]  goto timeout_arg
if [%1]==[-cfgsvc]   goto cfgsvc_arg
if [%1]==[-cp]       goto cp_arg
if [%1]==[-debug]    goto debug_arg
if [%1]==[-target]   goto target_arg
if %VALUE:~0,2%==-D  goto java_opts
set ARGS=%ARGS% %1
goto next_arg

Rem Add -D System Properties
:java_opts
Set PROP=%~1
shift
Set PROP=%PROP%=%1
Set JONAS_OPTS=%JONAS_OPTS% %PROP%
goto next_arg

:cfgsvc_arg
shift
if [%1]==[] goto cfgsvc_usage
set WHERE=cfgsvc
set WRAPPER_CONF=%1
goto start_arg

:start_arg
set MODE=start
set CLASS_TO_RUN=org.objectweb.jonas.server.Server
echo JONAS_BASE is set to %JONAS_BASE%
goto next_arg

:stop_arg
set MODE=stop
set CLASS_TO_RUN=org.objectweb.jonas.adm.JonasAdmin -s
goto next_arg

:admin_arg
set MODE=admin
set CLASS_TO_RUN=org.objectweb.jonas.adm.JonasAdmin
echo JONAS_BASE is set to %JONAS_BASE%
goto next_arg

:version_arg
set MODE=version
set CLASS_TO_RUN=org.objectweb.jonas_lib.version.Version
goto next_arg

:check_arg
set MODE=check
set CLASS_TO_RUN=org.objectweb.jonas.tools.CheckEnv
echo JONAS_BASE is set to %JONAS_BASE%
goto next_arg

Rem Take all arguments after ntservice and goto ntservice processing
:ntservice_arg
set MODE=ntservice
shift
set ARGS=
:nt_args
if [%1]==[] goto nt_args_done
set ARGS=%ARGS% %1
shift
goto nt_args

:nt_args_done
echo JONAS_BASE is set to %JONAS_BASE%
goto ntservice

:fg_arg
set WHERE=foreground
goto next_arg

:bg_arg
set WHERE=background
goto next_arg

:win_arg
set WHERE=window
goto next_arg

:n_arg
shift
set JONASNAME=%1
set NAME_OPT=-n %JONASNAME%
set JONAS_OPTS=%JONAS_OPTS% -Djonas.name=%JONASNAME%
goto next_arg

:timeout_arg
shift
set PINGTIMEOUT=%1
set PINGTIMEOUT_OPT=-timeout %PINGTIMEOUT%
goto next_arg

:cp_arg
shift
set CLASSPATH=%CLASSPATH%;%~1
goto next_arg

:debug_arg
shift
if not [%1]==[-p] goto debug_usage
shift
set JONAS_DEBUG_PORT=%1
set JONAS_DEBUG_SUSPEND=n
if not [%2]==[-s] goto set_debug_opts
shift
shift
set JONAS_DEBUG_SUSPEND=%1

:set_debug_opts
echo JOnAS Debug Info :
echo  listening on port : %JONAS_DEBUG_PORT%
echo  suspend mode : %JONAS_DEBUG_SUSPEND%
set JONAS_DEBUG_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=%JONAS_DEBUG_PORT%,suspend=%JONAS_DEBUG_SUSPEND%
goto next_arg

:target_arg
if [%MODE%]==[start] set ARGS=%ARGS% -start
if [%MODE%]==[stop] set ARGS=%ARGS% -stop
set MODE=admin
set ARGS=%ARGS% -target
goto next_arg

:next_arg
shift
if not [%1]==[] goto loop_on_args

if [%MODE%]==[] goto no_mode

Rem ---------------------------------------------
Rem Remove RMI Annotation
Rem ---------------------------------------------
set JONAS_OPTS=%JONAS_OPTS% -Djava.rmi.server.RMIClassLoaderSpi=org.objectweb.jonas.server.RemoteClassLoaderSpi

Rem ---------------------------------------------
Rem Set tomcat/jetty base directory
Rem ---------------------------------------------
set TOMCAT_OPTS=
set JETTY_OPTS=
if not ["%TOMCAT_BASE%"]==[""] set TOMCAT_OPTS=%TOMCAT_OPTS% -Dtomcat.base="%TOMCAT_BASE%"
if not ["%CATALINA_HOME%"]==[""] set TOMCAT_OPTS=%TOMCAT_OPTS% -Dcatalina.home="%CATALINA_HOME%"
if not ["%CATALINA_BASE%"]==[""] set TOMCAT_OPTS=%TOMCAT_OPTS% -Dcatalina.base="%CATALINA_BASE%"
if not ["%JETTY_HOME%"]==[""] set JETTY_OPTS=-Djetty.home="%JETTY_HOME%"

Rem ---------------------------------------------
Rem Run java command
Rem ---------------------------------------------
set BOOT=org.objectweb.jonas.server.Bootstrap
if not [%MODE%]==[start] goto admin
if %WHERE%==cfgsvc goto cfgsvc_mode
if %WHERE%==foreground goto start_jonas_fg
if %WHERE%==background goto start_jonas_bg
if %WHERE%==window     goto start_jonas_window
goto :EOF

:cfgsvc_mode
rem called by ANT create_win32service
set JAVA_CMD=%JAVA% -classpath %JONAS_ROOT%/lib/client.jar org.objectweb.jonas.tools.GenerateWrapperConf
%JAVA_CMD% -d ; -i 2 wrapper.java.classpath %CLASSPATH% >%WRAPPER_CONF%
%JAVA_CMD% -i 2 wrapper.java.additional %JAVA_OPTS% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% >>%WRAPPER_CONF%
echo wrapper.app.parameter.10=%JONASNAME% >>%WRAPPER_CONF%
echo wrapper.ntservice.name=JOnAS_%JONASNAME% >>%WRAPPER_CONF%
echo wrapper.ntservice.displayname=JOnAS (%JONASNAME%) >>%WRAPPER_CONF%
goto :EOF

:start_jonas_bg
REM start JOnAS in Background mode
start /B "JOnAS" %JAVA% %JAVA_OPTS% %JONAS_DEBUG_OPTS% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% %BOOT% %CLASS_TO_RUN%
REM Wait until JOnAS is online
%JAVA% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% %BOOT% org.objectweb.jonas.adm.JonasAdmin -ping %NAME_OPT% %PINGTIMEOUT_OPT%
goto :EOF

:start_jonas_fg
REM start JOnAS in foreground
%JAVA% %JAVA_OPTS% %JONAS_DEBUG_OPTS% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% %BOOT% %CLASS_TO_RUN%
goto :EOF

:start_jonas_window
set WINDOW_TITLE="%HOSTNAME%:%JONASNAME%"
if %WINDOW_TITLE%==":" set WINDOW_TITLE="JOnAS Server"
REM start %WINDOW_TITLE% %JAVA% %JAVA_OPTS% %JONAS_DEBUG_OPTS% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% %BOOT% %CLASS_TO_RUN%
start %WINDOW_TITLE% jonas start -fg
goto :EOF

:admin
%JAVA% %JONAS_OPTS% %TOMCAT_OPTS% %JETTY_OPTS% %BOOT% %CLASS_TO_RUN% %NAME_OPT% %PINGTIMEOUT_OPT% %ARGS%
goto :EOF

:ntservice
%JONAS_ROOT%\bin\nt\jonasnt %ARGS%
goto :EOF

:setjava
echo JAVA_HOME not set.
goto :EOF

:no_mode
echo "No startup mode specified, specify start | stop | admin | version | check"
goto usage

:no_arg
echo No arguments specified.
goto usage

:usage
echo "jonas start | stop | admin | version | check | ntservice | -fg | -bg | -win | -n | -debug | -cfgsvc | -cnhost | -cnport"
echo "Debug mode : jonas start -debug -p <debug-port> [-s <suspend:y/n>]"
REM TODO: Add here explanation for the arguments
goto :EOF

:setroot
echo JONAS_ROOT not set.
goto :EOF

:cfgsvc_usage
echo -cfgsvc option requires filename for target wrapper.conf file
goto :EOF

:debug_usage
echo -debug option parameters are : "-debug -p <debug-port> [-s <suspend:y/n>]"
goto :EOF
