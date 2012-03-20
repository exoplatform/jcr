#!/bin/bash

args=("$@")

if [ "$1" != "-u" ] 
then
echo "           -u <user> -p <password> [form_of_authentication] <host:port> <command> "
echo " "
echo "           [form_of_authentication]  :  -b - is used for basic authentication "
echo "                                        -f [-c <context>] - is used for form authentication with context portal if parameter context not specified "
echo "                                        if no authentication set basic authentication is used"
echo "           -c <context>              :  context, by default context is portal"
echo "           <command>                 :  start <repo[/ws]> <backup_dir> [<incr>]  "
echo "                                        stop <backup_id> "
echo "                                        status <backup_id> "
echo "                                        restores <repo[/ws]> "
echo "                                        restore [remove-exists] {{<backup_id>|<backup_set_path>} | {<repo[/ws]> {<backup_id>|<backup_set_path>} [<pathToConfigFile>]}} "
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
echo " "
echo "           All valid combination of parameters for command restore: "
echo "             1. restore remove-exists <repo/ws> <backup_id>       <pathToConfigFile> "
echo "             2. restore remove-exists <repo>    <backup_id>       <pathToConfigFile> "
echo "             3. restore remove-exists <repo/ws> <backup_set_path> <pathToConfigFile> "
echo "             4. restore remove-exists <repo>    <backup_set_path> <pathToConfigFile> "
echo "             5. restore remove-exists <backup_id> "
echo "             6. restore remove-exists <backup_set_path> "
echo "             7. restore <repo/ws> <backup_id>       <pathToConfigFile> "
echo "             8. restore <repo>    <backup_id>       <pathToConfigFile> "
echo "             9. restore <repo/ws> <backup_set_path> <pathToConfigFile> "
echo "            10. restore <repo>    <backup_set_path> <pathToConfigFile> "
echo "            11. restore <backup_id> "
echo "            12. restore <backup_set_path> "
exit 1
fi

user="$2"
pass="$4"

if [ "$5" = "-f" ]
then

  if [ "$6" = "-c" ]
  then
    context="$7"
    host=${8#*"http://"}
    newargs="http://$host/$context/rest form POST /$context/login?initialURI=/$context/private&username=$user&password=$pass ${args[@]:8}"
  else
    context="portal"
    host=${6#*"http://"}
    newargs="http://$host/$context/rest form POST /$context/login?initialURI=/$context/private&username=$user&password=$pass ${args[@]:6}"
  fi

else
  if [ "$5" = "-b" ]
  then
    host=${6#*"http://"}
    newargs="http://$user:$pass@$host/rest/private ${args[@]:6}"
  else
    host=${5#*"http://"}
    newargs="http://$user:$pass@$host/rest/private ${args[@]:5}"
  fi
fi

./jcrbackup.sh $newargs
