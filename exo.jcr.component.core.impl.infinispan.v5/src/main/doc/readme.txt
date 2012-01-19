To launch the cache server the script corresponding to your OS startCacheServer.cmd on windows
environment and startCacheServer.sh on unix/linux/macos environments followed by the next optional arguments:

help|?|<configuration-file-path>|udp|tcp <initial-hosts>

           help                      :  Print the expected syntax.
           ?                         :  Print the expected syntax.
           configuration-file-path   :  The location of the configuration file to use, we expect an absolute path. It will try to get it using the current class loader, 
                                        if it cannot be found it will get it from the file system. By default it will use the path /conf/cache-server-configuration.xml
                                        that is actually a file bundled into the jar.
           udp                       :  We use this parameter value when we want to use the default configuration file with udp as transport stack which is actually
                                        the default stack used which means that it will have the exact same behavior as when we don't provide any parameter.
           tcp                       :  We use this parameter value when we want to use the default configuration file with tcp as transport stack.
           initial-hosts             :  This parameter is optional and is only allowed in case the tcp stack is enabled, it will allow you to define the set of
                                        hosts that will be part of the cluster. The syntax of this parameter is a list of hostname[port] comma-separated. 
                                        Knowing that the default value is "localhost[7800],localhost[7801]" if this parameter is not set, the bind address will be
                                        automatically set to 127.0.0.1.
                                        
