JCR 1.14.0-Beta03 release notes
===========================

eXoPlatform Java Content Repository (JSR-170) implementation and Extension services with clustering support.

Features of 1.14.0-Beta03
- Possibility to create new repository in runtime in cluster environment
- Support easy restore without manual cleaning database and value storage
- Possibility to start all the cluster nodes in parallel even during for the first initialization of the JCR
- Repository backup support in Backup Console tool
- Limited support WedDav ACL specification

Changes of 1.14.0-Beta03
====================

Bug
    * [EXOJCR-956] - Cannot use webdav service with a version of jcr:content
    * [EXOJCR-961] - On restore node the versioning child was removed, but versioning child should be remain unchanged.
    * [EXOJCR-974] - Simple skin from examples folder doesn't appear at list
    * [EXOJCR-977] - InitialContextBinder.java doesn't support // instances
    * [EXOJCR-979] - Lock operations does not checks any permission
    * [EXOJCR-982] - Problems during testing of jetty: *ERROR* [qtp31131058-61] PropFindResponseEntity:
    * [EXOJCR-984] - CachingContainer returns unregistered components
    * [EXOJCR-993] - Daily testing problem: NPE during testing
    * [EXOJCR-994] - WEBDav view in a webbrowser has hardcoded image links
    * [EXOJCR-995] - NPE when try to import data via WorkspaceContentImporter
    * [EXOJCR-1000] - Not correct MBean components registration when PortalContainer contains more then one repository 
    * [EXOJCR-1001] - refresh breaks webdav published files
    * [EXOJCR-1002] - MethodAccessFilter return not human readable error message and don't set "JAXRS-Body-Provided" header in case rolles not allowed
    * [EXOJCR-1003] - DeleteCommand in case try remove locked file return don't human readable message 
    * [EXOJCR-1012] - Response on PROPFIND method with 'allprop' element contains wrong formatted and extra data
    * [EXOJCR-1013] - Response on PROPFIND method with 'allprop' element doesn't contain properties inside <D:include> element
    * [EXOJCR-1014] - FTP server doesn't show list of workspaces after repository restoring
    * [EXOJCR-1015] - PROBLEM with Testing Backup Console: restore workspace/backup
    * [EXOJCR-1030] - Impossible to retrieve the lock on a node even by the root
    * [EXOJCR-1036] - Empty metadata field not updated when re-uploading documents
    * [EXOJCR-1037] - If a header delegate is not available then toString method of Object should be used
    * [EXOJCR-1038] - Some Unit Tests on DB2 related to the CAS plugin fail
    * [EXOJCR-1040] - eXo JCR doesn't work with Oracle 11g R2 RAC
    * [EXOJCR-1043] - Index already present error during cluster start
    * [EXOJCR-1051] - Problems when start backup-console
    * [EXOJCR-1058] - Problem with uploading files via MacOS client
    * [EXOJCR-1060] - Indexer doesn't fully release resources on stop
    * [EXOJCR-1061] - Access Denied in jcr:content with anonim__ permission
    * [EXOJCR-1062] - WebDAV response is LOCKED after LOCK -> PUT request from the client (but PUT sends locktoken given after LOCK)
    * [EXOJCR-1065] - Functional testing jcr.core: API Tests failures on Mysql DB
    * [EXOJCR-1069] - Backup console throws NPE if backup agent isn't deployed on server
    * [EXOJCR-1073] - TestRPCServiceImpl fails with JGroups 2.10.0.GA
    * [EXOJCR-1085] - Wrong Content-Type header for files over a certain size
    * [EXOJCR-1087] - MySQL-UTF8 dialect default collation can be case-insensitive - need case-sensitive set explicitly
    * [EXOJCR-1092] - Unexpected behaviour of Nautilus while creating/renaming a folder/file via WebDAV 
    * [EXOJCR-1093] - Wrong pasting of a copied folder using Nautilus via WebDAV
    * [EXOJCR-1095] - Webdav doesn't work on JBoss
    * [EXOJCR-1098] - The If-Modified-Since property in the HTTP header doesn't exist
    * [EXOJCR-1107] - RPCService.executeCommandOnCoordinator() doesn't work properly when few instances are on the same workstation
    * [EXOJCR-1109] - Object BooleanValue must return string representation of boolean value by method getStringValue().
    * [EXOJCR-1113] - SessionDataManager.listChildPropertiesData(NodeData parent) must not return ValueDatas
    * [EXOJCR-1115] - Exception error during edit content
    * [EXOJCR-1117] - Correct misspelling in some methods names: doPriviledged to doPrivileged
    * [EXOJCR-1118] - IncrementalBackupJob should be thread safe
    * [EXOJCR-1131] - Node restore result depends on cache eviction
    * [EXOJCR-1133] - Unit tests fail due to a AccessControlException in cluster mode
    * [EXOJCR-1139] - Restored version nodes get id from jcr:uuid property instead fetch generated id
    * [EXOJCR-1140] - Corrupted data if the server is stopped while document is locked
    * [EXOJCR-1145] - IndexMerger in RO mode is not aware of new indexes
    * [EXOJCR-1151] - FORM authentication doesn't work with jcrbackup tool and Platform
    * [EXOJCR-1152] - WADL generation fails if resource class contains some type of sub-resource locators
    * [EXOJCR-1154] - Repository restore fails using exobackup tool with Component unregister error
    * [EXOJCR-1155] - Set scope test for junit dependency in exo.ws.rest.ext project
    * [EXOJCR-1165] - BackupManager restore : Temporary files not deleted
    * [EXOJCR-1169] - Remove some unnecessary jars
    * [JCR-1462] - Problems during testing of backup on jcr 1.12.5-GA-SNAPSHOT
    * [JCR-1483] - When a folder is cut server->client, folder on server doesn't remove.
    * [JCR-1485] - Unknown error and strange behavior when mary edits a webcontent
    * [JCR-1508] - Property not found dc:title during testing RandomReadNtFileWithMetadataTest

Documentation
    * [JCR-1433] - jcr backup/restore

Feature Request
    * [EXOJCR-747] - Make Backup restore easier
    * [EXOJCR-887] - Allow to start all the cluster nodes in parallel even during for the first initialization of the JCR
    * [EXOJCR-971] - Prevent unauthorized access to the methods of RepositoryServiceImpl
    * [EXOJCR-973] - Add possibility set user role in org.exoplatform.services.rest.tools.ResourceLauncher 
    * [EXOJCR-992] - Allow to use a String as entity for a JSON response
    * [EXOJCR-997] - Whole Repository backup support in Backup Console tool
    * [EXOJCR-999] - Limit the total amount of WorkerThreads
    * [EXOJCR-1011] - Allow to use variables to define the realm, the rest context and the portal container name of a PortalContainerDefinition
    * [EXOJCR-1020] - Create groovy compiler which able consume JCR node references and produce set of classes as result.
    * [EXOJCR-1024] - Make possible to overwrite default providers (readers and writers)
    * [EXOJCR-1029] - The result of the method getReferencesData is never stored into the cache
    * [EXOJCR-1032] - Limited support WedDav ACL specification 
    * [EXOJCR-1064] - Code review of ACL managment in case of copy/moving nodes
    * [EXOJCR-1066] - Avoid iterating over a List thanks to its iterator when it is possible
    * [EXOJCR-1079] - Create plugin for adding exception mapping providers in REST environment
    * [EXOJCR-1099] - Add support restore existed repository/workspace in backup console
    * [EXOJCR-1105] - Make it possible to configure dependencies path for Groovy scripts in runtime
    * [EXOJCR-1143] - Add possibility to get raw JSON data in methods of RESTful services.
    * [EXOJCR-1150] - DB clean on MySQL should not use iterating over all db 
    * [EXOJCR-1164] - Make possible in JrGroovyCompiler get list of URL on dependency classes
    * [EXOJCR-1172] - Rest framework must understand http header "x-forwarded-host"

Task
    * [EXOJCR-946] - Adopt Backup client article
    * [EXOJCR-949] - Updating eXo JCR version in ra.xml automatically 
    * [EXOJCR-952] - Backport 1.14.x branch commits to trunk
    * [EXOJCR-955] - BufferedJBossCache performance improving
    * [EXOJCR-963] - [DOC]Reply on JBC regions to avoid having too many JBC instances
    * [EXOJCR-965] - Cache doesn't support disable feature
    * [EXOJCR-972] - Add org.exoplatform.services.rest.ext.method.filter.MethodAccessFilter in container configuration by default
    * [EXOJCR-981] - Remove timestamp from names of jar-files in application bundles
    * [EXOJCR-986] - Enable the security manager by default in all the projects
    * [EXOJCR-988] - RepositoryException: URI is not hierarchical on remove workspace via backup console
    * [EXOJCR-989] - Refactor JSON framework
    * [EXOJCR-996] - Merge performance improvements
    * [EXOJCR-998] - Null values management must be reviewed to be up to date
    * [EXOJCR-1010] - Create two more constructors for WebDavServiceImpl
    * [EXOJCR-1018] - Bind slf4j-log4j and log4j dependencies onto the test phase of the various modules of JCR [part #2]
    * [EXOJCR-1028] - Update the example of configuration for a cluster environment
    * [EXOJCR-1044] - Check TestQueryUsecases
    * [EXOJCR-1054] - Apply all the changes made in the ISPN branch to the trunk
    * [EXOJCR-1055] - Update XPath query ordering chapter in jcr documents
    * [EXOJCR-1067] - Reduce contention on read in NodeTypeDataHierarchyHolder if possible
    * [EXOJCR-1070] - Remove dependency on pull-parser artifact as redundant
    * [EXOJCR-1071] - Move JCR framework commands list configuration to it right place
    * [EXOJCR-1072] - Publish the reference guide in docbook format
    * [EXOJCR-1074] - Avoid iterating over a List thanks to its iterator when it is possible [Part #2]
    * [EXOJCR-1075] - JCR backupset should be fully independent
    * [EXOJCR-1076] - Backup console binary distribution
    * [EXOJCR-1083] - Create Group personnal folder should be after Group creation: aplly patch
    * [EXOJCR-1084] - Exclude jgroups:jgroups:jar:2.6.13.GA:compile dependency from org.jboss.cache:jbosscache-core:jar:3.2.6.GA:compile in JCR parent pom.xml
    * [EXOJCR-1086] - Remove permission on child isn't used: apply patch
    * [EXOJCR-1114] - Support more MIME types
    * [EXOJCR-1119] - Avoid to get an item from the DB if the parent node is new
    * [EXOJCR-1120] - Changes log traversing is under optimized
    * [EXOJCR-1129] - Port the documentation about the ISPN implementation of eXo Cache
    * [EXOJCR-1134] - Check in IndexerSingletonStoreCacheLoader if the children should not be removed
    * [EXOJCR-1135] - Check in DefaultChangesFilter if we use the right ids in case of a IOException while updating the index of the parentSearchManager
    * [EXOJCR-1137] - Remove some unnecessary jars
    * [EXOJCR-1138] - Document databases supported by eXo JCR
    * [EXOJCR-1148] - Add posibility to configure additional DBCP DataSources parameters in RepositoryCreationService
    * [EXOJCR-1166] - Document Repository or Workspace initialization from backup 
    * [EXOJCR-1171] - Make possible to use the same GroovyClassLoader in JcrGroovyCompiler and GroovyJaxrsPublisher
    * [JCR-1515] - Remove jgroup dependency from jcr-services as redundant

Sub-task
    * [EXOJCR-928] - Study the way how repository configuration can be replicable
    * [EXOJCR-929] - Implementation
    * [EXOJCR-939] - Create service to clean DB
    * [EXOJCR-966] - Write the specification of the RPCService
    * [EXOJCR-967] - Implement the RPCService
    * [EXOJCR-1091] - Try to find way to restore value more than 2G
    * [EXOJCR-1096] - Test on all supported database
    * [EXOJCR-1097] - Support of restore independent of mulit-db type
    * [EXOJCR-1101] - Check TestRDBMSBAckupManager
    * [EXOJCR-1102] - RDBMS backup stress testing
    * [EXOJCR-1106] - Profiling backup/restore method
    * [EXOJCR-1110] - Dump data from system table in case of multi-db for non system table
    * [EXOJCR-1121] - HSQLDB performs commit after tables are locked
    * [EXOJCR-1127] - Make workspace waiting before starting full backup job
    * [EXOJCR-1130] - Implement approach using the marker Backupable
    * [EXOJCR-1146] - DBCleanerService should not relate on AbstractCacheableLockManager
    * [EXOJCR-1147] - Make backup/restore to be cluster aware
    * [JCR-1481] - Adding support form authentication in backup console

Changes of 1.14.0-Beta02
====================

Bug
    * [EXOJCR-688] - Some entries in the eXo JCR cache are not evicted properly
    * [EXOJCR-843] - Exceptions after importing file with version history
    * [EXOJCR-849] - "Permission denied" on client side, when trying to move file(s) to another workspace through FTP
    * [EXOJCR-856] - Problems while recopying same files via webdav
    * [EXOJCR-865] - Data corrupt after restore a node which has been imported with version history
    * [EXOJCR-878] - WebDAV doesn't work with nt:file
    * [EXOJCR-879] - TestCaching.testNotModifiedSince failed in same cases
    * [EXOJCR-882] - TestCaching fails on Windows XP SP 2 with Russian locale
    * [EXOJCR-888] - The problems with restore version node
    * [EXOJCR-890] - JSON framework don't work with beans created in groovy
    * [EXOJCR-891] - Snaphosts IDs make the applications build improperly
    * [EXOJCR-897] - Add registration required node types in single DB confg for test TestImport.
    * [EXOJCR-908] - Used wrong delimiter during parsing permission value
    * [EXOJCR-909] - In LDAPService, InitialContext is not safely closed in authenticate method
    * [EXOJCR-912] - Unable to convert the JCR documentation to pdf
    * [EXOJCR-916] - Duplicate instantiation of some services
    * [EXOJCR-921] - Workspace.copy(srcWS, srcAbsPath, destAbsPath) can not copy root child to another workspace root child
    * [EXOJCR-922] - MapResourceBundle.resolveDependencies() throw java.lang.StackOverflowError
    * [EXOJCR-924] - Unable to coerce 'Event' into a LONG: java.lang.NumberFormatException: For input string: "Event"
    * [EXOJCR-933] - Determine property type from nodetype definition in DocumentViewImport for version history.
    * [EXOJCR-936] - Avoid converting binary value to String in tests
    * [EXOJCR-954] - Can't get property of a node if it has a child node with the same name with the property
    * [EXOJCR-964] - User logged-out and cannot login after some inactivity

Feature Request
    * [EXOJCR-749] - Make eXo JCR rely on Apache Tika
    * [EXOJCR-771] - Jetty Support
    * [EXOJCR-776] - Implement the method toString for the main classes of JCR for debugging purpose
    * [EXOJCR-842] - Allow to disable a given PortalContainer
    * [EXOJCR-880] - Determine property is multi or single value from nodetype definition in import.
    * [EXOJCR-886] - Update the document handler to manage MS Office 2007 meta data extraction (docx, ...)
    * [EXOJCR-934] - Decouple event name from listener name in ListenerService.
    * [EXOJCR-935] - Add "dav:isreadonly" property management
    * [EXOJCR-942] - Reply on JBC regions to avoid having too many JBC instances
    * [EXOJCR-943] - Make JBC implementation of eXo Cache replies on JBC regions to avoid having too many JBC instances
    * [EXOJCR-950] - Prevent the JobSchedulerServiceImpl to launch jobs that rely on non started services in JBoss AS
    * [EXOJCR-951] - Make the JobSchedulerServiceImpl support multi portal containers

Task
    * [EXOJCR-752] - Avoid to load into the memory the full content of a document while extracting the metadata and the text content
    * [EXOJCR-755] - Study the extensibility of Apache Tika
    * [EXOJCR-910] - Resynchronize the doc of JCR 1.12 with the trunk
    * [EXOJCR-917] - core.packaging.module.js error when in deploy phase
    * [EXOJCR-919] - maxVolatileTime should be checked on checkFlush()
    * [EXOJCR-927] - Add "application/x-groovy+html" to HTMLDocumentReader and "application/x-jaxrs+groovy" to TextPlainDocumentReader
    * [EXOJCR-957] - Remove organization nodetypes from projects where it not used
    * [EXOJCR-962] - [DOC]Make JBC implementation of eXo Cache replies on JBC regions to avoid having too many JBC instances

Sub-task
    * [EXOJCR-892] - Remove Fake Chapters
    * [EXOJCR-893] - Apply the structure of PLF
    * [EXOJCR-940] - Created special method for remove workspace without checking of system workspace
    * [EXOJCR-941] - Create special method for remove repository without checking of default-repository 


Changes of 1.14.0-Beta01
====================

Bug
    * [EXOJCR-564] - Cannot use the old Hibernate org service in Gate In
    * [EXOJCR-570] - AddNamespacePlugin registers namespaces after repostiory start
    * [EXOJCR-638] - get mixin types through the NodeTypeUtil class
    * [EXOJCR-662] - Processing SQLException may cause infinite loop.
    * [EXOJCR-667] - Temporary spooled file can be not found on save
    * [EXOJCR-698] - URL encoding in SEARCH and PROPFIND responces differs.
    * [EXOJCR-699] - DAILY TESTS are going too long
    * [EXOJCR-713] - org.exoplatform.services.rest.impl.RequestDispatcher must return readable messages if resource not found or HTTP method is not allowed for resource
    * [EXOJCR-754] - JDBC Statements left open : Use of Datasources instead of DBCP and C3P0 pools
    * [EXOJCR-763] - Reordering samename sibling nodes does not update path of child nodes
    * [EXOJCR-766] - QPath isDescendantOf returns wrong result on samename siblings
    * [EXOJCR-768] - A session should not be useable after a logout
    * [EXOJCR-774] - If-Modified-Since doesn't seem to be well managed in the Wevdav Component
    * [EXOJCR-781] - LockManagerImpl should call InitialContextInitializer.recall
    * [EXOJCR-784] - DOC : wrong examples in profiles section
    * [EXOJCR-785] - Parameter maxVolatileTime is not working correctly
    * [EXOJCR-788] - Inconsistency issue cans occur on default portal container parameters
    * [EXOJCR-794] - Field "CONFIG" in the table "JCR_CONFIG" is too short on MySql
    * [EXOJCR-795] - Unexpected behavior of the method PortalContainer.isScopeValid()
    * [EXOJCR-796] - Data corruption
    * [EXOJCR-804] - "No such file or directory" exception for value storage when using MySQL or Postgres DB in WCM demo 2.0
    * [EXOJCR-805] - Can not search user with keyword that contain special character
    * [EXOJCR-806] - Problems while copying "ftp-ftp"
    * [EXOJCR-810] - TestRemoveFromValueStorage failed in configuration without ValueStorage
    * [EXOJCR-812] - InitialContextBinder.bind should be thread-safe
    * [EXOJCR-813] - ItemImpl.getParent method must return session pooled parent
    * [EXOJCR-817] - max-buffer-size from configuration should be use to TransientValueData in import (docview and sysview)
    * [EXOJCR-819] - HTTPBackupAgent doesn't provide information about last successfully restored repositories if restore was launched in synchronous mode
    * [EXOJCR-825] - Problems with functional testing - multi, value-storage is turned off
    * [EXOJCR-835] - TestMultiDbJDBCConnection and TestSingleDbJDBCConnection must drop also JCR_xCONTAINER table on tearDown
    * [EXOJCR-837] - FUNCTIONAL testing jcr.ext - TestBackupManager in error
    * [EXOJCR-840] - java.util.ConcurrentModificationException on org.exoplatform.services.jcr.ext.hierarchy.impl.NewUserListener.processUserStructure
    * [EXOJCR-844] - JCR inside application server is not started due to missing component
    * [EXOJCR-857] - Exception during PROPFIND request if some property content "%" and after not hex chracters
    * [EXOJCR-859] - Random failed tests during building jcr
 
Feature Request
    * [EXOJCR-156] - Disable deleting of the workspace which is set as system
    * [EXOJCR-157] - Disable deleting of the repository which is set as default
    * [EXOJCR-190] - Support returning directly Collection<T> for MediaType.APPLICATION_JSON
    * [EXOJCR-230] - Refactore and move in main part of exo.ws.rest.core project class AbstractResourceTest
    * [EXOJCR-311] - Make broadcasting of events of ListenerService asynchronous
    * [EXOJCR-420] - Check if the ItemReferencePool of the SessionDataManager can be implemented with WeakValueHashMap instead of a WeakHashMap
    * [EXOJCR-482] - Be able to load a class stored in another groovy file
    * [EXOJCR-498] - Provide more details when a JCR query is invalid
    * [EXOJCR-517] - filter to authenticate a signed request from gadgets
    * [EXOJCR-609] - Allow to keep missing values into the JCR Cache
    * [EXOJCR-626] - H2 Database support
    * [EXOJCR-634] - Upload of a file with special characters like " ' " in filename is not supported by the FTPservice
    * [EXOJCR-635] - Avoid unneccesary checks in persistence
    * [EXOJCR-745] - Allow cascading imports in configuration files
    * [EXOJCR-750] - JCR path management improvement
    * [EXOJCR-782] - No longer force extension developers to redefine the whole dependencies list
    * [EXOJCR-786] - The method that registers plugins should be overloadable
    * [EXOJCR-793] - Make possible to configure permissions for Groovy REST services when the SecurityManager is instaled
    * [EXOJCR-797] - Unable see error message from ProxyService if remote server does not provide Content-Type header.
    * [EXOJCR-822] - Make implementation of MethodInvokerFilter which can disable access to methods of RESTful services to any users except services deployer
    * [EXOJCR-823] - Make possible to provide optional attributes for RESTful resources
    * [EXOJCR-824] - Make expiring mechanism for temporary "under development" services
    * [EXOJCR-864] - Add method GroovyScript2RestLoader.load with the same signature as it is in 1.12.x
 
Patch
    * [EXOJCR-772] - SharedStateLoginModule does swallow an exception during login phase
 
Task
    * [EXOJCR-392] - Siblings reordering may update not all the child-items in cache
    * [EXOJCR-542] - Improve error message concerning a missing "Query Manager Factory"
    * [EXOJCR-618] - BufferedJBossCache Optimisation: research how to use internal ChangeList as non-persistent cache inside opened transaction
    * [EXOJCR-689] - Standartize eXo JCR docnmentation projects description. 
    * [EXOJCR-691] - Fix your missing dependencies
    * [EXOJCR-746] - Remove CommunicationService
    * [EXOJCR-756] - Make JCR core work properly when the Security Manager is installed
    * [EXOJCR-764] - Fix the security issue about the JCR System Session
    * [EXOJCR-767] - Prevent unauthorized access to the method ConversationState.setCurrent(ConverstionState state)
    * [EXOJCR-770] - Prevent modification of user's identity without required permissions
    * [EXOJCR-775] - Rework RESTRegistryService for using current repository only. Rewove repository name from PathParam.
    * [EXOJCR-777] - Prevent modifications of ConversationState's attributes without required permissions
    * [EXOJCR-778] - Protect the main methods of a repository since they are critical
    * [EXOJCR-779] - Provide current ConversationState for anonymous user also.
    * [EXOJCR-780] - Move org.exoplatform.services.jcr.access.SystemIdentity class from exo.jcr.core exo.kernel or exo.core
    * [EXOJCR-783] - Use cached table for HSLQLDB tables
    * [EXOJCR-791] - SwapFile and SpoolFile tests
    * [EXOJCR-807] - Port the article Groovy Scripts as REST Services
    * [EXOJCR-808] - For Sybase DB "check-sns-new-connection" should be set to false by default
    * [EXOJCR-809] - OrganizationService's tests should not be excluded
    * [EXOJCR-815] - Document how to use AS Managed DataSource
    * [EXOJCR-845] - Remove exo:audit* nodetypes from configuration.
 
Sub-task
    * [EXOJCR-627] - Investigate use of in-memory databases for in-memory JCR
    * [EXOJCR-742] - Port Workspace Data Container articles into docbook
    * [EXOJCR-757] - Training on Java Security
    * [EXOJCR-758] - Implement it
    * [EXOJCR-759] - Test it
    * [EXOJCR-790] - OS depended paths in policy files for tests.
    * [EXOJCR-798] - Cluster testing security support
    * [EXOJCR-799] - MySQL & PostgreSQL tesing problem with security enabled
    * [EXOJCR-851] - Searching users with special characters in name does not work properly
    * [EXOJCR-862] - Check Property.getStream() method
    * [EXOJCR-863] - Check VersionHistory.removeVersionLabel() & VersionHistory.addVersionLabel()



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
* eXo Kernel 2.3.0-Beta03
* eXo Core 2.4.0-Beta03
* eXo WS 2.2.0-Beta03
* eXo JCR 1.14.0-Beta03

Find all 1.14 task on JIRA:
https://jira.jboss.org/jira/browse/EXOJCR

1.14.0-Beta03 tasks:
http://jira.exoplatform.org/browse/JCR/fixforversion/12699
https://issues.jboss.org/browse/EXOJCR/fixforversion/12315430

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
 