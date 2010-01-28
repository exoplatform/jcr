DEPRECATED, use multiplexer-based configuration!

INFO

This is new, template-based configuration of JCR with JBossCache. 
This configuration uses MySQL database (sql script for creating
and deleting all needed DBs is shown below).

TEMPLATING
Configuration templates should use variables like:
"jbosscache-*" or "jgroups-configuration" any other wouldn't be 
inserted into the template.



DROP  DATABASE IF EXISTS portal ;
create database portal default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS jcr ;
create database jcr default charset latin1 collate latin1_general_cs;

DROP  DATABASE IF EXISTS jcr2 ;
create database jcr2 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS jcr3 ;
create database jcr3 default charset latin1 collate latin1_general_cs;

DROP  DATABASE IF EXISTS jcrtest ;
create database jcrtest default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS portaltck ;
create database portaltck default charset latin1 collate latin1_general_cs;

DROP  DATABASE IF EXISTS jcrtck ;
create database jcrtck default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS jcr2tck;
create database jcr2tck default charset latin1 collate latin1_general_cs;

DROP  DATABASE IF EXISTS jdbcjcr2export1 ;
create database jdbcjcr2export1 default charset latin1 collate latin1_general_cs;
DROP  DATABASE IF EXISTS jdbcjcr2export2 ;
create database jdbcjcr2export2 default charset latin1 collate latin1_general_cs;

DROP  DATABASE IF EXISTS jdbcjcr2export3 ;
create database jdbcjcr2export3 default charset latin1 collate latin1_general_cs;
