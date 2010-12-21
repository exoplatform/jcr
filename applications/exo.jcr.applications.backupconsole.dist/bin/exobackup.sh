#!/bin/bash

args=("$@")

if [ "$1" != "-u" ] 
then
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
echo "           remove-exists             :  remove fully (db, value storage, index) exists repository/workspace "
echo "           force-close-session       :  close opened sessions on repository or workspace "
exit 1 
fi

user="$2"
pass="$3"
host=${5#*"http://"}
if [ "$4" = "-f" ]
then
  newargs="http://$host/portal/rest form POST /portal/login?username=$user&password=$pass ${args[@]:5}"
else
  if [ "$4" = "-b" ]
  then
    newargs="http://$user:$pass@$host ${args[@]:5}"
  fi
fi

./jcrbackup.sh $newargs
