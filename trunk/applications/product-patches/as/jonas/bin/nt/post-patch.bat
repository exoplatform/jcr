@echo off
Rem eXo / JOnAS Post installation Patch
Rem This patch moves the appropriate file to complete the installation

Rem Keep variables local to this script
setlocal ENABLEDELAYEDEXPANSION

Rem %~f0 is the script path
@set this_fqn=%~f0
@for %%i in ( !this_fqn! ) do @set bin_nt_dir=%%~dpi
@for %%i in ( !bin_nt_dir!\.. ) do @set bin_dir=%%~dpi
@for %%i in ( !bin_dir! ) do @set JONAS_ROOT=%%~dpi

if exist %JONAS_ROOT%\rars\autoload\joram_for_jonas_ra.rar goto joram
echo [PATCH] Nothing to do
goto xmlapis

:joram
move %JONAS_ROOT%\rars\autoload\joram_for_jonas_ra.rar %JONAS_ROOT%\rars\
echo [PATCH] Moving joram_for_jonas_ra.rar from autoload

:xmlapis
if exist %JONAS_ROOT%\lib\endorsed\xml-apis.jar goto mvxmlapis
echo [PATCH] Nothing to do
goto end

:mvxmlapis
move %JONAS_ROOT%\lib\endorsed\xml-apis.jar %JONAS_ROOT%\lib\endorsed\xml-apis.jar.backup
echo [PATCH] Renaming xml-apis.jar to xml-apis.jar.backup

:end
echo [PATCH] Post patch complete
