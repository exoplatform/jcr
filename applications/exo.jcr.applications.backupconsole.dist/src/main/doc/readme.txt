jcrbackup.cmd and jcrbackup.sh - suitable for Standalone and flexible for various authentication ways shell scripts

           <url_basic_authentication> | <url_form_authentication>  <command> 

           <url_basic_authentication>:  http(s)//login:password@host:port/<context> 
           <url_form_authentication> :  http(s)//host:port/<context> <form_auth_parm> 

           <form_auth_part>          :  form <method> <form_path>
           <method>                  :  POST or GET
           <form_path>               :  /path/path?<paramName1>=<paramValue1>&<paramName2>=<paramValue2>...

           Example of <url_form_authentication> - http://127.0.0.1:8080/portal/rest form POST "/portal/login?initialURI=/portal/private&username=root&password=gtn"

           <command>                 :  start <repo[/ws]> <backup_dir> [<incr>] 
                                        stop <backup_id>
                                        status <backup_id>
                                        restores <repo[/ws]>
                                        restore [remove-exists] [<repo[/ws]>] {<backup_id>|<backup_set_path>} [<pathToConfigFile>]
                                        list [completed]
                                        info
                                        drop [force-close-session] <repo[/ws]>
                                        help

           start                     :  start backup of repository or workspace
           stop                      :  stop backup
           status                    :  information about the current or completed backup by 'backup_id'
           restores                  :  information about the last restore on specific repository or workspace
           restore                   :  restore the repository or workspace from specific backup
           list                      :  information about the current backups (in progress)
           list completed            :  information about the completed (ready to restore) backups
           info                      :  information about the service backup
           drop                      :  delete the repository or workspace
           help                      :  print help information about backup console

           <repo[/ws]>               :  /<repository-name>[/<workspace-name>]  the repository or workspace
           <backup_dir>              :  path to folder for backup on remote server
           <backup_id>               :  the identifier for backup
           <incr>                    :  incremental job period
           <pathToConfigFile>        :  path (local) to  repository or workspace configuration
           remove-exists             :  remove fully (db, value storage, index) exists repository/workspace
           force-close-session       :  close opened sessions on repository or workspace


exobackup.sh and exobackup.cmd - suitable for use with GateIn based products like Platform

           -u <user> -p <password> [form_of_authentication] <host:port> <command>

           <form_of_authentication>  :  -b - is used for basic authentication
                                        -f [-c <context>] - is used for form authentication with context portal if parameter context not specified 
                                        if no authentication set basic authentication is used
           -c <context>              :  context, by default context is portal

           <command>                 :  start <repo[/ws]> <backup_dir> [<incr>] 
                                        stop <backup_id>
                                        status <backup_id>
                                        restores <repo[/ws]>
                                        restore [remove-exists] [<repo[/ws]>] {<backup_id>|<backup_set_path>} [<pathToConfigFile>]
                                        list [completed]
                                        info
                                        drop [force-close-session] <repo[/ws]>
                                        help

           start                     :  start backup of repository or workspace
           stop                      :  stop backup
           status                    :  information about the current or completed backup by 'backup_id'
           restores                  :  information about the last restore on specific repository or workspace
           restore                   :  restore the repository or workspace from specific backup
           list                      :  information about the current backups (in progress)
           list completed            :  information about the completed (ready to restore) backups
           info                      :  information about the service backup
           drop                      :  delete the repository or workspace
           help                      :  print help information about backup console

           <repo[/ws]>               :  /<repository-name>[/<workspace-name>]  the repository or workspace
           <backup_dir>              :  path to folder for backup on remote server
           <backup_id>               :  the identifier for backup
           <incr>                    :  incremental job period
           <pathToConfigFile>        :  path (local) to  repository or workspace configuration
           remove-exists             :  remove fully (db, value storage, index) exists repository/workspace
           force-close-session       :  close opened sessions on repository or workspace
