This configuration is designed for cluster mode. 

** USAGE **
To use this configuration set, replace following line in pom.xml
 <jcr.test.configuration.file>/conf/cluster/test-configuration.xml</jcr.test.configuration.file> 

** DATABASE **
It uses MySQL databases,
that can be created with following script (appendix A). MySQL password currently is set
to "admin", please change it in order to be suitable for Your's MySQL 
configuration.

** INDEXER **
Indexer on "WS" is configured to use JBossCacheChangesFilter, that pushes lists of
added/removed nodes and indexes through the separate instance of JBoss Cache.
Please configure index-dir of query handler to the same (*shared?) folder for
each cluster node (i.e. "/tmp/jcr-clustering/index/db1/ws" or "/mnt/server-nfs/index/db1/ws").
<property name="index-dir" value="target/temp/index/db1/ws" /> 

Appendix A: DB script

DROP  DATABASE IF EXISTS cluster_jcr1 ;
create database cluster_jcr1 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr2 ;
create database cluster_jcr2 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr3 ;
create database cluster_jcr3 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr4 ;
create database cluster_jcr4 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr5 ;
create database cluster_jcr5 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr6 ;
create database cluster_jcr6 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr7 ;
create database cluster_jcr7 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr8;
create database cluster_jcr8 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr9 ;
create database cluster_jcr9 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS cluster_jcr10 ;
create database cluster_jcr10 default charset latin1 collate latin1_general_cs;