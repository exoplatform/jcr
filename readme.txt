JCR 1.12.6-CR01 release notes
===========================

eXoPlatform Java Content Repository (JSR-170) implementation and Extension services with clustering support.

Features of this version:
* Whole Repository backup/restore support in Backup Console tool
* The result of the method getReferencesData is stored into cache
* Allow to use a String as entity for a JSON respons

Changes of 1.12.6-CR01
=====================

Bug
    * [JCR-1470] - refresh breaks webdav published files
    * [JCR-1482] - Corrupted Data if the server is stopped while somebobdy is editing a document in ECMS
    * [JCR-1485] - Unknown error and strange behavior when mary edits a webcontent
    * [JCR-1490] - Some Unit Tests on DB2 related to the CAS plugin fail
    * [JCR-1494] - FTP server doesn't show list of workspaces after repository restoring

Documentation
    * [JCR-1433] - jcr backup/restore

Improvement
    * [KER-164] - Allow to use variables to define the realm, the rest context and the portal container name of a PortalContainerDefinition
    * [WS-256] - Allow to use a String as entity for a JSON response
    * [JCR-1459] - Whole Repository backup support in Backup Console tool
    * [JCR-1469] - JCR clustering consumes lot of native threads
    * [JCR-1491] - The result of the method getReferencesData is never stored into the cache

Task
    * [JCR-1489] - eXo JCR doesn't work with Oracle 11g R2 RAC
    * [JCR-1495] - Bind slf4j-log4j and log4j dependencies onto the test phase of the various modules of JCR [part #2]

Sub-task
    * [JCR-1481] - Adding support form authentication in backup console

Changes of 1.12.5-GA
=====================

Bug
    * [KER-162] - Simple skin from examples folder doesn't appear at list
    * [KER-163] - CachingContainer returns unregistered components
    * [COR-213] - User logged-out and cannot login after some inactivity
    * [WS-254] - Add org.exoplatform.services.rest.ext.method.filter.MethodAccessFilter in container configuration by default
    * [JCR-1438] - Problem with JCR versionning
    * [JCR-1449] - Can't get property of a node if it has a child node with the same name with the property
    * [JCR-1450] - JCROrganizationService contains nodetype with same name as in CS
    * [JCR-1453] - Missed nodetypes in cluster testing configuration
    * [JCR-1462] - Problems during testing of backup on jcr 1.12.5-GA-SNAPSHOT
    * [JCR-1466] - RepositoryException: URI is not hierarchical on remove workspace via backup console
    * [JCR-1474] - NPE when try to import data via WorkspaceContentImporter

Improvement
    * [KER-160] - Prevent the JobSchedulerServiceImpl to launch jobs that rely on non started services in JBoss AS
    * [KER-161] - Make the JobSchedulerServiceImpl support multi portal containers

Task
    * [JCR-1455] - Doc's title should be rename from eXoJCR Reference Manual to eXo JCR Developer Guide
    * [JCR-1461] - Remove timestamp from names of jar-files in application bundles
    * [JCR-1467] - Cannot use webdav service with a version of jcr:content
    * [JCR-1471] - Updating eXo JCR version in ra.xml automatically
    * [JCR-1472] - Adopt Backup client article
    * [JCR-1473] - merge performance improvements

Changes of 1.12.4-GA
=====================

Bug
    * [EXOJCR-688] - Some entries in the eXo JCR cache are not evicted properly
    * [EXOJCR-843] - Exceptions after importing file with version history
    * [EXOJCR-849] - "Permission denied" on client side, when trying to move file(s) to another workspace through FTP
    * [EXOJCR-856] - Problems while recopying same files via webdav
    * [EXOJCR-865] - Data corrupt after restore a node which has been imported with version history
    * [EXOJCR-878] - WebDAV doesn't work with nt:file
    * [EXOJCR-879] - TestCaching.testNotModifiedSince failed in same cases
    * [EXOJCR-888] - The problems with restore version node
    * [EXOJCR-890] - JSON framework don't work with beans created in groovy
    * [EXOJCR-891] - Snaphosts IDs make the applications build improperly
    * [EXOJCR-908] - Used wrong delimiter during parsing permission value
    * [EXOJCR-909] - In LDAPService, InitialContext is not safely closed in authenticate method
    * [EXOJCR-912] - Unable to convert the JCR documentation to pdf
    * [EXOJCR-916] - Duplicate instantiation of some services
    * [EXOJCR-921] - Workspace.copy(srcWS, srcAbsPath, destAbsPath) can not copy root child to another workspace root child
    * [EXOJCR-924] - Unable to coerce 'Event' into a LONG: java.lang.NumberFormatException: For input string: "Event"
    * [EXOJCR-933] - Determine property type from nodetype definition in DocumentViewImport for version history.
    * [EXOJCR-936] - Avoid converting binary value to String in tests

Feature Request
    * [EXOJCR-842] - Allow to disable a given PortalContainer
    * [EXOJCR-880] - Determine property is multi or single value from nodetype definition in import.
    * [EXOJCR-886] - Update the document handler to manage MS Office 2007 meta data extraction (docx, ...)
    * [EXOJCR-934] - Decouple event name from listener name in ListenerService.
    * [EXOJCR-935] - Add "dav:isreadonly" property management

Task
    * [EXOJCR-896] - Port Manageability article into docbook
    * [EXOJCR-905] - Merge the reference guide and the user guide in one single guide
    * [EXOJCR-913] - Abuse of INFO level logging for DocNumberCache.get()
    * [EXOJCR-914] - excessive INFO logging by IndexMerger.run()
    * [EXOJCR-915] - excessive INFO logging by IndexMerger.run()
    * [EXOJCR-917] - core.packaging.module.js error when in deploy phase
    * [EXOJCR-919] - maxVolatileTime should be checked on checkFlush()
    * [EXOJCR-927] - Add "application/x-groovy+html" to HTMLDocumentReader and "application/x-jaxrs+groovy" to TextPlainDocumentReader
    * [EXOJCR-892] - Remove Fake Chapters

Changes of 1.12.3-GA
=====================
 
Bug
    * [EXOJCR-754] - JDBC Statements left open : Use of Datasources instead of DBCP and C3P0 pools
    * [EXOJCR-763] - Reordering samename sibling nodes does not update path of child nodes
    * [EXOJCR-766] - QPath isDescendantOf returns wrong result on samename siblings
    * [EXOJCR-774] - If-Modified-Since doesn't seem to be well managed in the Wevdav
    * [EXOJCR-781] - LockManagerImpl should call InitialContextInitializer.recall
    * [EXOJCR-784] - DOC : wrong examples in profiles section
    * [EXOJCR-785] - Parameter maxVolatileTime is not working correctly
    * [EXOJCR-788] - Inconsistency issue cans occur on default portal container parameters
    * [EXOJCR-795] - Unexpected behavior of the method PortalContainer.isScopeValid()
    * [EXOJCR-796] - Data corruption
    * [EXOJCR-804] - "No such file or directory" exception for value storage when using MySQL or Postgres DB in WCM demo 2.0
    * [EXOJCR-806] - Problems while copying "ftp-ftp"
    * [EXOJCR-810] - TestRemoveFromValueStorage failed in configuration without ValueStorage
    * [EXOJCR-813] - ItemImpl.getParent method must return session pooled parent
    * [EXOJCR-817] - max-buffer-size from configuration should be use to TransientValueData in import (docview and sysview)
    * [EXOJCR-835] - TestMultiDbJDBCConnection and TestSingleDbJDBCConnection must drop also JCR_xCONTAINER table on tearDown
    * [EXOJCR-857] - Exception during PROPFIND request if some property content "%" and after not hex chracters
    * [EXOJCR-865] - Data corrupt after restore a node which has been imported with version history
    * [EXOJCR-882] - TestCaching fails on Windows XP SP 2 with Russian locale
 
Feature Request
    * [EXOJCR-230] - Refactore and move in main part of exo.ws.rest.core project class AbstractResourceTest
    * [EXOJCR-782] - No longer force extension developers to redefine the whole dependencies list
    * [EXOJCR-783] - Use cached table for HSLQLDB tables
    * [EXOJCR-797] - Unable see error message from ProxyService if remote server does not provide Content-Type header.
 
Task
    * [EXOJCR-392] - Siblings reordering may update not all the child-items in cache
    * [EXOJCR-751] - Prepare maintenance branch for jcr 1.12
    * [EXOJCR-808] - For Sybase DB "check-sns-new-connection" should be set to false by default
    * [EXOJCR-809] - OrganizationService's tests should not be excluded
    * [EXOJCR-815] - Document how to use AS Managed DataSource
    * [EXOJCR-867] - Port documentation for Kernel from wiki to docbook
    * [EXOJCR-868] - Port documentation for Core from wiki to docbook
    * [EXOJCR-869] - Port documentation for JCR from wiki to docbook
    * [EXOJCR-870] - Cleanup WS documentation
    * [EXOJCR-871] - Document RestServicesList service
    * [EXOJCR-881] - Port functionality of EXOJCR-482 in jcr-1.12.x
    * [EXOJCR-884] - Rename JCR documentation artifacts to exo.jcr.* form

Changes of 1.12.2-GA
====================

Bug
    * [EXOJCR-497] - JCR serialization test wrong logic with CASable storage
    * [EXOJCR-730] - Restored repository not accessible after restart Tomcat
    * [EXOJCR-731] - Deploy error (500 - Unexpected error. null) of REST Service with annotation inheritance.
    * [EXOJCR-735] - JCR repositories created in runtime is not available after eXo Social restart
    * [EXOJCR-736] - Problems with anonymous entrance on FTP and NPE
    * [EXOJCR-743] - InitialContextBinder bind twice same datasource in some case
    * [EXOJCR-762] - Check whether the repository with the given name doesn't exists before starting restore from backup

Feature Request
    * [EXOJCR-640] - Migrate to newer version of Apache PDFBox ( and FontBox ) if possible;

Task
    * [EXOJCR-596] - Upload eXoJCR documentation on jboss.org
    * [EXOJCR-668] - Validate format of the default values of the property definition during the nodetype registration
    * [EXOJCR-738] - Search does not work with source in CDATA tag in XML document
    * [EXOJCR-740] - Constrains ranges are not used in NodeTypeImpl.canSetProperty() validation
    * [EXOJCR-741] - Backupconsole build improvements
    * [EXOJCR-765] - Use StringBuilder instead of String concatenation in MSExcelDocumentReader.getContentAsText
    * [EXOJCR-681] - Decreasing perfomance while running WebdavReadWriteTest tests several times in row



Changes of 1.12.2-CR1
=====================

Bug
    * [EXOJCR-175] - Problems with HTTPBackupAgent - Cyrillic symbols aren't showing after restore
    * [EXOJCR-683] - java.io.IOException: Socket read failed on heavy loaded WebdavAddBLOBTest benchmark test
    * [EXOJCR-697] - SQL search by date doesn't work
    * [EXOJCR-698] - URL encoding in SEARCH and PROPFIND responces differs.
    * [EXOJCR-700] - Problem in user search with MySql and PostgresDB
    * [EXOJCR-704] - JCR testuite hangs on sybase
    * [EXOJCR-708] - Problem with full text searching in text files with non-latin content.
    * [EXOJCR-712] - Concurrent service creation leads to duplicate service instantiation
    * [EXOJCR-724] - Bad URL in the error message when a component cannot be instantiated
    * [EXOJCR-726] - Improper conversion of jboss.server.config.url system property value into File (spaces in filename problem)
    * [EXOJCR-729] - The FileNotFoundException in restore workspace over BackupWorkspaceinitializer
    * [EXOJCR-734] - The binary values was not stored in incremental backup.

Feature Request
    * [EXOJCR-705] - Expose listeners in OrganizationService
    * [EXOJCR-707] - Check repository management operations on thread safety
    * [EXOJCR-718] - Allow to get the complete configuration at runtime
    * [EXOJCR-719] - Better debugging of components loaded
    * [EXOJCR-721] - Add possibility to use customized GroovyClassLoader in org.exoplatform.services.script.groovy.GroovyScriptInstantiator
    * [EXOJCR-722] - Make it possible to use other then org.exoplatform.services.rest.impl.method.DefaultMethodInvoker

Task
    * [EXOJCR-354] - Invoke post read after permissions check
    * [EXOJCR-663] - Make possibility extends classes RequestDispatcher and ResourceBinder.
    * [EXOJCR-691] - Fix your missing dependencies
    * [EXOJCR-692] - Find the reason why the method of type Node.hasNodes is much slower since beta5
    * [EXOJCR-694] - Change JBC dependencies to use 3.2.4.GA
    * [EXOJCR-696] - Reduce the concurrency Level in the JBoss Cache Config
    * [EXOJCR-711] - Misleading error message appears when the external settings cannot be found
    * [EXOJCR-714] - Improve the usability of the ContainerLifecyclePlugin
    * [EXOJCR-715] - Ensure that the ExoContainer is fully ThreadSafe
    * [EXOJCR-716] - Prevent the JobSchedulerServiceImpl to launch jobs that rely on non started services
    * [EXOJCR-717] - Add to RestRegistryService method without repositoryName in PathParam, insted use current repository. Methods with repositoryName in PathParam marks as Deprecated.
    * [EXOJCR-720] - Make possibility extends classe GroovyScript2RestLoader
    * [EXOJCR-723] - JCR Statistics: Describe the arguments of the methods exposed through JMX
    * [EXOJCR-728] - implementing RequestLifecycle for REST services


Changes of 1.12.1-GA
=====================

Bug
    * [EXOJCR-612] - JBoss Cache Implementation for the Cache Service test TestAbstractExoCache fails
    * [EXOJCR-638] - get mixin types through the NodeTypeUtil class
    * [EXOJCR-661] - Cannot access to the MBeans through the JConsole in Standalone mode
    * [EXOJCR-662] - Processing SQLException may cause infinite loop.
    * [EXOJCR-664] - org.exoplatform.services.jcr.impl.storage.value.fs.TestFileIOChannel.testConcurrentRead fail with MSSQL and DB2
    * [EXOJCR-667] - Temporary spooled file can be not found on save
    * [EXOJCR-671] - ConcurrentModificationException in FileCleaner with heavy load
    * [EXOJCR-672] - An eXoCache clear should be local
    * [EXOJCR-687] - Some JCR parameters that are time parameter are retrieved as number instead of time

Feature Request
    * [EXOJCR-498] - Provide more details when a JCR query is invalid
    * [EXOJCR-634] - Upload of a file with special characters like " ' " in filename is not supported by the FTPservice
    * [EXOJCR-645] - Add ExtHttpHeaders.JAXRS_BODY_PROVIDED header for unhandled exception in REST services and set error message to body responce

Task
    * [EXOJCR-578] - Use Fisheye in SCM urls used in maven
    * [EXOJCR-611] - Provide a way to collect statistics around the JCR API accesses
    * [EXOJCR-639] - Find the reason why the methods of type Property.setValue are much slower since beta5
    * [EXOJCR-685] - Change JBC dependencies to use 3.2.3.GA
    * [EXOJCR-689] - Standartize eXo JCR docnmentation projects description.
    * [EXOJCR-690] - Apply changes in the eXo JCR project in order to be able to publish artifacts in the nexus of JBoss
    * [EXOJCR-545] - Checking performance on SearchNodesByPropertyTest
    * [EXOJCR-643] - Improve the performances of the lucene indexing in a cluster by removing contention for read operations


Changes of 1.12.1-CR1
=====================

Bug
    * [EXOJCR-256] - There are server errors "500 Internal Server Error:" during creation repository or workspace by RestRepositoryService
    * [EXOJCR-348] - Test problem: TestCleanableFileStreamValueData failed
    * [EXOJCR-519] - DAILY TESTS are going too long (avg time=5hours)
    * [EXOJCR-531] - Problems with Lock operations
    * [EXOJCR-546] - TESTING: Performance testing problems. LockUnlockOwnNodeTest - TPS fell down
    * [EXOJCR-548] - problem with import & export node
    * [EXOJCR-555] - NPE with cache eviction at startup
    * [EXOJCR-557] - Problem while uploading *.pdf to WebDAV server using Mac OS Finder
    * [EXOJCR-558] - Files uploaded by Mac OS finder are displayed with size "0"
    * [EXOJCR-559] - Problems with daily performance testing - on PostgreSQL 8.2.9
    * [EXOJCR-567] - The REST servlet dump errors when the client cut the socket too early should be only a debug log
    * [EXOJCR-572] - Can not create workspace with default configuration of lock manager
    * [EXOJCR-581] - Listing the directory in TreeFile may return null during race condition, causing NPE.
    * [EXOJCR-584] - User's research is case sensitive
    * [EXOJCR-586] - Missed slf4j dependency for jcr applications on tomcat AS
    * [EXOJCR-587] - session.save() throws NPE after node reordering
    * [EXOJCR-588] - Tests errors in eXo XML Processing Services on MACOS
    * [EXOJCR-591] - Problem with ObservationManager
    * [EXOJCR-599] - deadlock during dashboard editing
    * [EXOJCR-600] - Concurrency problem (java.util.HashMap.put called from CacheableLockManagerImpl.getSessionLockManager(CacheableLockManagerImpl.java:473))
    * [EXOJCR-601] - gatein sample extension should not be required
    * [EXOJCR-602] - StackOverflow on JsonGeneratorImpl
    * [EXOJCR-603] - impossible to change user password
    * [EXOJCR-607] - Sybase Issue with GateIn
    * [EXOJCR-608] - XaSessionImpl as XA resource should be unique per user, workspace and repository
    * [EXOJCR-614] - Node.getReferences fail in some cases
    * [EXOJCR-615] - Need check nodedata to avoid exception in method NodeImpl.isNodeType(String).
    * [EXOJCR-619] - Log record forging (Security Issue)
    * [EXOJCR-621] - Conflict between symlink feature and Jbosscache
    * [EXOJCR-623] - Unable to get a version of document using WebDAV (HTTP Response 404 returned).
    * [EXOJCR-633] - Problems with manual testing - tomcat-server on ftp -.IndexOutOfBoundsException

Feature Request
    * [EXOJCR-549] - Backup and Restore of a whole Repository
    * [EXOJCR-571] - Change PersitedValueDataReader/Writer
    * [EXOJCR-573] - Create database and bind DataSource in runtime
    * [EXOJCR-582] - DB script modification for oracle11 compatibility
    * [EXOJCR-585] - Allow to get statistics on Database Access without using a Profiler
    * [EXOJCR-616] - Remove repository container from repositoryContainers map when repository container start fail.
    * [EXOJCR-617] - Map environment parameters for all String fields in Repository configuration

Task
    * [EXOJCR-150] - Ftp client tests failute
    * [EXOJCR-250] - Add human readable message in case Workspace creation error via HTTPBackupAgent
    * [EXOJCR-393] - Create indexer load test
    * [EXOJCR-523] - Upgrade to JBoss Cache 3.2.3.GA
    * [EXOJCR-550] - Bind slf4j-log4j and log4j dependencies onto the test phase of the various modules of JCR
    * [EXOJCR-552] - Allow to Test eXo JCR 1.12 on EC2
    * [EXOJCR-575] - Remove unused PairChangesLog class
    * [EXOJCR-589] - Limit network traffic and thread blocking for the Lucene Indexer in a cluster
    * [EXOJCR-590] - DO NOT exclude tests from a parent pom
    * [EXOJCR-598] - Allow to disable the hints used for the Complex Queries on oracle
    * [EXOJCR-605] - Normalize logging categories
    * [EXOJCR-631] - Find a reason, why functional tests fails under Tornado.MySQL with "Cannot create PoolableConnectionFactory (Too many connections)" message.
    * [EXOJCR-632] - svn: File 'jcr.packaging.module/1.12.0-CP01/jcr.packaging.module-1.12.0-CP01.js' has inconsistent newlines


Features of eXoJCR 1.12 comparing to 1.11
=========================================

- Repository clustering based on JBossCache and JBoss Transactions. 
- Lazy-load option for child nodes and properties read, improved items dataflow for read/write operations
- Alternative data container optimized for read operations (consuming less database queries)
- Database dialect can be autodetected (if not pointed in the configuration)
- Support for Values large of 2GiB
- Portal container configuration improvements (default definitions, link and externaly loaded parameters) 
- Concurrency improvements for Session registry and Values stroage
- Concurrency improvements for XA transactions support (Repository login and logout faster now)
- Improved serach based on Lucene 2.4
- Support of MySQL/InnoDB database for multi-language content
- Standalone container can use configuration stored in JBossAS server configuration directory by default 
- WebDAV server update-policy can be configured to different versioning behaviour
- Lot of WebDAV server bugfixes
- HTTP (RESTful) Backup agent with concole client
- HTTP (RESTful) Repository management service
- Support of Java6 and Java5 runtime and development environment

Since version of 1.12 eXoJCR available under LGPL license (version 2.1).

eXoJCR 1.12 tested in on the databases:
  MySQL 5.1 MYSQL Connector/J 5.1.8
  Oracle DB 10g (10.2.0.1) Oracle 10g (10.2.0.1)
  PostgresSQL 8.3.7 JDBC4 Driver, Version 8.3-605
  DB2 9,7 IBM Data Server Driver for JDBC and SQLJ (JCC Driver) Version: 9.1 (fixpack 3a)
  MS SQL Server 2005 SP3 JDBC Driver 2.0
  MS SQL Server 2008 SP1 JDBC Driver 2.0  
  Sybase 15.0.2 JConnect v6.0.5 (Build 26564 / 11 Jun 2009) 


Release includes:
* eXo Kernel 2.2.6-CR01
* eXo Core 2.3.6-CR01
* eXo WS 2.1.6-CR01
* eXo JCR 1.12.6-CR01

1.12.6-CR01 tasks:
* http://jira.exoplatform.org/browse/KER/fixforversion/12337
* http://jira.exoplatform.org/browse/COR/fixforversion/12338
* http://jira.exoplatform.org/browse/WS/fixforversion/12339
* http://jira.exoplatform.org/browse/JCR/fixforversion/12336

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

    * Copy jcr.ear into $jboss_home/server/default/deploy
    * Create $jboss_home/server/default/conf/exo-conf folder if it doesn't exist.
    * Put exo-configuration.xml into $jboss_home/server/default/conf/exo-conf/exo-configuration.xml
    * Configure JAAS by inserting XML fragment shown below into $jboss_home/server/default/conf/login-config.xml

---------
<application-policy name="exo-domain">
 <authentication>
      <login-module code="org.exoplatform.services.security.j2ee.JbossLoginModule" flag="required"></login-module>
  </authentication>
 </application-policy>
---------

  2. Start Up

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
 