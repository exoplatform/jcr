echo off
if %1 NEQ "-u" goto :help
set user=%2
set pass=%3
set auth=%4
set host=%5
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
set comm=%*

if %auth% == "-b" set newarg="http://%user%:%pass%@%host% %comm%" 
if %auth% == "-f" set newarg="http://%host%/portal/rest form POST /portal/login?username=%user%&password=%pass% %comm%"

jcrbackup.cmd %newarg%

exit
:help
echo "           -u <user> <password> <form_of_authentication> <host:port> <command> "
echo " "
echo "           <form_of_authentication>  :  -b - is used for basic authentication "
echo "                                        -f - is used for form authentication "
echo " "
echo "           <command>                 :  start <repo[/ws]> <backup_dir> [<incr>]  "
echo "                                        stop <backup_id> "
echo "                                        status <backup_id> "
echo "                                        restores <repo[/ws]> "
echo "                                        restore [remove-exists] [<repo[/ws]>] {<backup_id>|<backup_set_path>} [<pathToConfigFile>] "
echo "                                        list [completed] "
echo "                                        info "
echo "                                        drop [force-close-session] <repo[/ws]> "
echo "                                        help "
echo " "
echo "           start                     :  start backup of repository or workspace "
echo "           stop                      :  stop backup "
echo "           status                    :  information about the current or completed backup by 'backup_id' "
echo "           restores                  :  information about the last restore on specific repository or workspace "
echo "           restore                   :  restore the repository or workspace from specific backup "
echo "           list                      :  information about the current backups (in progress) "
echo "           list completed            :  information about the completed (ready to restore) backups "
echo "           info                      :  information about the service backup "
echo "           drop                      :  delete the repository or workspace "
echo "           help                      :  print help information about backup console "
echo " "
echo "           <repo[/ws]>               :  /<repository-name>[/<workspace-name>]  the repository or workspace "
echo "           <backup_dir>              :  path to folder for backup on remote server "
echo "           <backup_id>               :  the identifier for backup "
echo "           <incr>                    :  incremental job period "
echo "           <pathToConfigFile>        :  path (local) to  repository or workspace configuration "
echo "           remove-exists             : remove fully (db, value storage, index) exists repository/workspace "
echo "           force-close-session       :  close opened sessions on repository or workspace "




