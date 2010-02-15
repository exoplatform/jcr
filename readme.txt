JCR 1.12.0-CR2 release notes
============================

eXoPlatform Java Content Repository (JSR-170) implementation and Extension services with clustering support.

Features of eXoJCR 1.12.0 comparing to 1.11.2:
- Repository clustering based on JBossCache and JBoss Transactions. 
- Lazy-load option for child nodes and properties read, improved items dataflow for read/write operations
- Alternative data container optimized for read operations (consuming less database queries)
- Database dialect can be autodetected (if not pointed in the configuration)
- Support for Values large of 2GiB
- Concurrency improvements for Session registry and Values stroage
- Improved serach based on Lucene 2.4
- WebDAV server update-policy can be configured to different versioning behaviour
- Lot of WebDAV server bugfixes
- HTTP (RESTful) Backup agent with concole client
- HTTP (RESTful) Repository management service
- Support of Java6 and Java5 runtime and development environment

eXoJCR 1.12.0 tested in on the many databases:
  MySQL 5.1 MYSQL Connector/J 5.1.8
  Oracle DB 10g (10.2.0.1) Oracle 10g (10.2.0.1)
  PostgresSQL 8.3.7 JDBC4 Driver, Version 8.3-605
  DB2 9,7 IBM Data Server Driver for JDBC and SQLJ (JCC Driver) Version: 9.1 (fixpack 3a)
  MS SQL Server 2005 SP3 JDBC Driver 2.0
  MS SQL Server 2008 SP1 JDBC Driver 2.0  
  Sybase 15.0.2 JConnect v6.0.5 (Build 26564 / 11 Jun 2009) 

eXoPlatform Java Content Repository (JSR-170) implementation and
Extension services.
Includes:
* eXo JUnit Framework 1.2.1-CR2
* eXo Kernel 2.2.0-CR2
* eXo Core 2.3.0-CR2
* eXo WS 2.1.0-CR2
* eXo JCR 1.12.0-CR2

Find all 1.12.0 task on JIRA:
https://jira.jboss.org/jira/browse/EXOJCR

CR2 tasks:
https://jira.jboss.org/jira/browse/EXOJCR/fixforversion/12314456


JCR Samples
===========

1. Start Up (Tomcat)
   Tomcat 6 bundled can be started by executing the following commands:

      $CATALINA_HOME\bin\eXo.bat run          (Windows)

      $CATALINA_HOME/bin/eXo.sh run           (Unix)

2. After startup, the sample applications will be available by visiting:

    http://localhost:8080/browser - Simple JCR browser
        Browse the JCR repository that was started with Tomcat
    http://localhost:8080/fckeditor - FCK editor sample
        Edits the sample node using FCKEditor and browse it JCR browser
    http://localhost:8080/rest/jcr/repository/production - WebDAV service,
        Open in Microsoft Explorer, File-Open-OpenAsWebFolder with url http://localhost:8080/rest/jcr/repository/production
        Add/read/remove files there and browse it in the JCR browser or FTP.
        User name/password: root/exo
    ftp://localhost:2121 - FTP server
        Open the repository in FTP client and browse the JCR repository started with Tomcat as FTP content,
        add/read/remove files there and browse it in the JCR browser or WebDAV.

EAR deploy
==========

eXo JCR was tested under JBoss-5.1.0.GA application server

JBoss-5.1.0.GA

  1. Configuration

    * Copy <jcr.ear> into <%jboss_home%/server/default/deploy>
    * Put exo-configuration.xml to the root <%jboss_home%/exo-configuration.xml
    * Configure JAAS by inserting XML fragment shown below into <%jboss_home%/server/default/conf/login-config.xml>

---------
<application-policy name="exo-domain">
 <authentication>
      <login-module code="org.exoplatform.services.security.j2ee.JbossLoginModule" flag="required"></login-module>
  </authentication>
 </application-policy>
---------

  2. [Only for JBossAS 4.x] replace <%jboss_home%/server/default/lib/hsqldb.jar> with newest one from this distribution (hsqldb-1.8.0.7.jar).
  3. Start Up

     Execute
       * bin/run.bat on Windows
     or
       * bin/run.sh  on Unix

Resources
=========

 Company site        	http://www.exoplatform.com
 Documentation wiki   	http://wiki.exoplatform.org
 Community JIRA      	https://jira.jboss.org/jira/browse/EXOJCR, http://jira.exoplatform.org
 Comminity site      	http://www.exoplatform.org
 Community forum     	http://www.exoplatform.com/portal/public/en/forum			     
 JavaDoc site        	http://docs.exoplatform.org
 