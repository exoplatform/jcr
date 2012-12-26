Thank you for your interest in eXo JCR

To launch it, go to the bin directory and execute the following command:

In Unix environment

* "./eXo-ispn.sh run" to launch eXo JCR with the configuration for Infinispan
* "./eXo-jbc.sh jbc run" to launch eXo JCR with the configuration for JBoss Cache

In Windows environment

* "eXo-ispn.bat run" to launch eXo JCR with the configuration for Infinispan
* "eXo-jbc.bat run" to launch eXo JCR with the configuration for JBoss Cache

Notice!
JCR Bundles for Infinispan and JBoss Cache differs by their dependencies. 
In ordrer to run eXo JCR with Infinispan configuration it must be deployed 
with "ispn" profile.