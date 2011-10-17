JCR 1.14.2-GA release notes
===========================

eXoPlatform Java Content Repository (JSR-170) implementation and Extension services with clustering support.

Features of 1.14.2-GA
* Compressing the full content of backup

Changes of 1.14.2-GA
====================

Bug
    * [EXOJCR-1552] - 10 tests failing in exo-jcr testsuite with MySQL
    * [EXOJCR-1570] - WorksapceName not handled by SessionActionInterceptor
    * [EXOJCR-1578] - Group Id is null in GroupDAOImpl#preSave(Group group, boolean isNew)
    * [EXOJCR-1579] - Exo jcr testsuite failing with db2
    * [EXOJCR-1586] - The query findPropertyById is slow in single db mode with HSQLDB

Enhancement
    * [EXOJCR-1195] - Improve reindexing mechanism for RDBMS
    * [EXOJCR-1532] - Change the target xsd on xml file
    * [EXOJCR-1549] - Case-sensitivy problem with MSSQL in exo-jcr testsuite
    * [EXOJCR-1560] - Improve storing membership records in OrgService cache
    * [EXOJCR-1562] - Make it possible to send observation events even if session is used after logout
    * [EXOJCR-1564] - Don't make mandatory the parameters related to the incremental backup
    * [EXOJCR-1565] - Allow to zip the full content of a backup
    * [EXOJCR-1569] - Set properly the relativePath in the pom files

Quality Risk
    * [EXOJCR-1186] - Decrease performance of JCR 1.14.x since JCR 1.12.x

Task
    * [EXOJCR-852] - Review all excluded tests
    * [EXOJCR-953] - Propose a TCK for the OrganizationService
    * [EXOJCR-1498] - Migration tool and guideline for migration from 1.12.10-GA to 1.14.0-CR4-CP01
    * [EXOJCR-1577] - Configure the BackupManager by default in the bundles of eXo JCR in standalone mode

Sub-task
    * [EXOJCR-1506] - Improve RDBMS reindexing for MSSQL
    * [EXOJCR-1531] - Improve restoring operation for single-db configuration by optimization cleaning db
    * [EXOJCR-1543] - Improve query for Oracle DB

Changes of 1.14.1-GA
====================

Bug
    * [EXOJCR-1441] - Unable to run exo-jcr testsuite on oracle databases
    * [EXOJCR-1516] - ExtendedNodeTypeManager#unregisterNodeTypes(String[]) and ExtendedNodeTypeManager#unregisterNodeType(String) remove nodetypes in memory only.
    * [EXOJCR-1521] - ORA-00918 caused by the query for RDBMS indexing
    * [EXOJCR-1527] - Correct the third party dependencies in packaging js
    * [EXOJCR-1530] - Connections leaks in LDAP OrganizationService
    * [EXOJCR-1533] - TESTING: Regression during daily testing.
    * [EXOJCR-1537] - Database cleaning failed with MutliDB configuration on MSSQL
    * [EXOJCR-1540] - default property values produce an validation error with <int> and <long> tag
    * [EXOJCR-1541] - Transaction Timeout Exception while starting PLF 3.5
    * [EXOJCR-1544] - GateIn cannot run with Sybase database - indexing problem

Component Upgrade
    * [EXOJCR-1547] - Upgrade ISPN to 5.0.1 FINAL

Enhancement
    * [EXOJCR-1492] - Impove the method MembershipHandler.findAllMembershipsByGroup
    * [EXOJCR-1505] - Allow to resynchronize the lucene indexes of each cluster node at start up
    * [EXOJCR-1512] - no way to unregister listener on organization service
    * [EXOJCR-1546] - Downgrade Quartz to prevent incompatibility issues with EAP
    * [JCR-1669] - Support ComponentRequestLifecycle and reuse entities in JCR Organization service
    * [JCR-1670] - Add caching to JCR Organization service

Feature Request
    * [EXOJCR-1510] - no eventListener on MembershipType

Task
    * [EXOJCR-1493] - Write some doc about the ability to reindex a workspace asynchronously
    * [EXOJCR-1518] - Provide an Util method to give an alternative to TimeZone.getTimeZone with less contention
    * [EXOJCR-1519] - Replace TimeZone.getTimeZone with the util method from the kernel to have less contention
    * [EXOJCR-1520] - Contention on JCRDateFormat on heavy load

Sub-task
    * [EXOJCR-1487] - Improve restoring operation by removing constrains and triggers on db
    * [EXOJCR-1503] - Improve RDBMS reindexing for PgSQL
    * [EXOJCR-1507] - Improve RDBMS reindexing for DB2

Changes of 1.14.0-GA
====================

Bug
    * [EXOJCR-1203] - TestDocumentViewCollision sometime failed
    * [EXOJCR-1287] - RDBMS backup failed on Sybase because of constraint violation
    * [EXOJCR-1370] - Same name siblings is not managed correctly
    * [EXOJCR-1375] - TESTING: during load testing appear "Can not delete parent till childs exists" exception on RestoreOwnNodeTest
    * [EXOJCR-1379] - Malformed GET response body for non-latin named collections
    * [EXOJCR-1393] - Getting items by patterns doesn't work with MSSQL
    * [EXOJCR-1395] - Some functional test on DB2 fails with asserts and errors
    * [EXOJCR-1438] - Exception when edit the properties of a nt:resource node
    * [EXOJCR-1440] - ItemExistsException is thrown after update to JCR 1.14.0-CR3
    * [EXOJCR-1442] - Exo-jcr testsuite fully failing with MSSQL 2008
    * [EXOJCR-1457] - Empty multi-values properties should processed properly
    * [EXOJCR-1458] - getNodes doesn't return node ordered by order number in case of move
    * [EXOJCR-1459] - Add java.lang.Class to set of 'known types' for java to JSON transformation
    * [EXOJCR-1460] - RDBMS reindexing fails on database DB2 with multi-db.
    * [EXOJCR-1462] - FUNCTIONAL TESTING: ORA-00933: SQL command not properly ended
    * [EXOJCR-1463] - Undesirable mime-type resolver warning message while uploading files via ftp
    * [EXOJCR-1464] - System Session Provider should always return System Session
    * [EXOJCR-1465] - The namespace used in the configuration files is misspelled
    * [EXOJCR-1468] - Throw proper exception in method org.exoplatform.services.jcr.impl.core.NodeImpl.addNode(String path) if node type can't be determined.
    * [EXOJCR-1473] - WARNING: Could not unregister the MBean whenever server is shutdown
    * [EXOJCR-1474] - Fuzzy Search fail with some search keys
    * [EXOJCR-1485] - JCR backup should be portable
    * [EXOJCR-1496] - Need add annotation @Provider to the org.exoplatform.services.rest.ext.provider.HierarchicalPropertyEntityProvider 
    * [EXOJCR-1499] - Collision between cache regions on a cluster using several portal containers
    * [EXOJCR-1500] - Add Tika dependencies to Core packaging
    * [EXOJCR-1501] - Set minConnection and maxConnection params from the ldap configuration via system properties

Component Upgrade
    * [EXOJCR-1475] - Upgrade ISPN to 5.0.0 FINAL

Enhancement
    * [EXOJCR-1236] - MembershipHandler.linkMembership() should not allow to add membership record which already exists
    * [EXOJCR-1341] - Different behavior between Mysql and HSQL when creating contents with same name and different cases
    * [EXOJCR-1394] - Updated versions of JDBC Drivers for MSSQL and DB2 are no accessible on maven repositories
    * [EXOJCR-1411] - Make contact search in Organization Service non sensitive
    * [EXOJCR-1447] - Copy only jar-files in tomcat lib directory
    * [EXOJCR-1448] - Some code in jcr-ext require to be privileged 
    * [EXOJCR-1449] - Exclude dependencies of the eu.medsea.mimeutil artifact
    * [EXOJCR-1466] - Make possible get byte, short, int and other primitive types from StringValue
    * [EXOJCR-1472] - Performance issues met due to SecureRandom.nextBytes under heavy load
    * [EXOJCR-1480] - Improve performance of ldap organization service
    * [EXOJCR-1481] - Check index in READ_WRITE mode when setting it online
    * [EXOJCR-1483] - Set autocommit to false only for write operations
    * [EXOJCR-1491] - Add new method to MembershipHandler interface

Feature Request
    * [EXOJCR-267] - Implement Lazy Loading mechanism for the method Node.getNodes
    * [EXOJCR-1239] - Improve query with path for cases when node has a lot of children nodes
    * [EXOJCR-1389] - Improve Permission management in case of a JCR Query
    * [EXOJCR-1437] - Allow to choose a custom FSDirectory and FSDirectoryLockFactory
    * [EXOJCR-1467] - Concurrent repository creation
    * [EXOJCR-1478] - Performance issue with Oracle DB 11.2.0.2
    * [EXOJCR-1479] - Allow end-products to choose between SLF4J 1.6.* or 1.5.* versions
    * [EXOJCR-1486] - Support to disable value-storage via system property

Quality Risk
    * [EXOJCR-1406] - Regression during weekly performance testing - RandomReadNtFileWithMetadataTest
    * [EXOJCR-1446] - TESTING: Regression during load testing on LockUnlockOwnNodeTest

Task
    * [EXOJCR-923] - DefaultItemDataCopyVisitor refactoring
    * [EXOJCR-1315] - Align commons-collection depenency version in JCR subprojects
    * [EXOJCR-1390] - Message fix in NamespaceRegistry
    * [EXOJCR-1461] - Add a dependency in kernel packaging js
    * [EXOJCR-1470] - Allow top level applications to use any versions of commons-io
    * [EXOJCR-1476] - Remove all the explicit GC calls
    * [EXOJCR-1508] - Code Cleanup of JCR core

Sub-task
    * [EXOJCR-1361] - Profiling current patches to understand why we have some regression
    * [EXOJCR-1428] - Review TCK knows issues
    * [EXOJCR-1431] - Testing RDBMS features on MSSQL (2 DBs)
    * [EXOJCR-1432] - Testing RDBMS features on Sybase
    * [EXOJCR-1433] - Testing RDBMS features on DB2
    * [EXOJCR-1450] - Investigate the performance of different patches with original use case
    * [EXOJCR-1451] - Write tests
    * [EXOJCR-1452] - Implementation
    * [EXOJCR-1453] - Adoption and testing on all supported DB
    * [EXOJCR-1455] - Avoid getting unnecessary data from database
    * [EXOJCR-1488] - Use hints in query for reindixing for MySQL DB
    * [EXOJCR-1502] - Improve RDBBS reindexing for Sybase

Changes of 1.14.0-CR4
====================

Bug
    * [EXOJCR-1057] - Problems during testing of Lock operations (EditLockedCommonNodeTest)
    * [EXOJCR-1068] - Wrong unregister procedure for containers components
    * [EXOJCR-1132] - Check problem with save configuration (RepositoryServiceConfigurationImpl.retain) to file system on OS Windows. 
    * [EXOJCR-1191] - Check problem with compatibility of incremental backup for JCR 1.12.x to JCR 1.14.x.
    * [EXOJCR-1385] - PROBLEMS during functional testing: test TestISPNCacheWorkspaceStorageCacheInClusterMode
    * [EXOJCR-1392] - Delay in replication of Nodes data in JBoss EPP Cluster
    * [EXOJCR-1413] - [Perf] ItemDataTraversingVisitor#visit(NodeData node) continue to visit deeper althought onParentVersion=IGNORE
    * [EXOJCR-1414] - Lists stored into the cache can be inconsistent in cluster environment
    * [EXOJCR-1415] - FUNCTIONAL testing: failures on MySql
    * [EXOJCR-1434] - FileCleaner in BackupManagerImpl should be used from proper RepositoryContainer 
    * [EXOJCR-1436] - Index reader handling improvements

Enhancement
    * [EXOJCR-1162] - Remove duplicated classes
    * [EXOJCR-1303] - WARN No principal found when performing JBoss security manager cache eviction
    * [EXOJCR-1378] - The "mimetype" of a file without extension is not retrieved in upload

Feature Request
    * [EXOJCR-1374] - Multi DB Schema Support
    * [EXOJCR-1376] - Add UUID into the exception message content of ItemExistsException
    * [EXOJCR-1396] - Add posibility to remove repositories created by RepositoryCreationService
    * [EXOJCR-1397] - Create method which will return db configuration of repository
    * [EXOJCR-1398] - Add posibility create repository in RepositoryCreationService with custom set of DB connection parameters
    * [EXOJCR-1405] - Allow to set backup-dir in repository backup-log as related path or(and) can be set with help of system variables
    * [EXOJCR-1417] - Make the code fully independent of DBCP
    * [EXOJCR-1439] - Add getCurrentRepositoryName method to RepositoryServiceImpl

Quality Risk
    * [EXOJCR-1215] - Regression during daily tests on JCR-1.14.0

Task
    * [EXOJCR-1017] - Reduce amount of used threads as Search/Index engine
    * [EXOJCR-1283] - Generate a source release zip
    * [EXOJCR-1332] - SQL scripts for production
    * [EXOJCR-1404] - Deprecate StorageUpdateManager feature
    * [EXOJCR-1419] - Move FutureExoCache from GateIn commons to eXo kernel
    * [EXOJCR-1426] - Cleanup the code to get rid of useless System.currentTimeMillis() calls

Sub-task
    * [EXOJCR-1362] - Put nodes in cache with ACL when they are asked by UUID
    * [EXOJCR-1377] - Review tests in jcr-core projects [part 2]
    * [EXOJCR-1382] - Investigate the reason of decreasing Property.setValue() performance
    * [EXOJCR-1383] - Support paging for Sybase DB
    * [EXOJCR-1384] - Prepare DB for testing RDBMS reindxing/backup/restore features
    * [EXOJCR-1400] - Prepare Sybase DB for testing RDBMS features
    * [EXOJCR-1401] - Prepare DB2 for testing RDBMS features
    * [EXOJCR-1403] - Prepare Oracle 11 R1 and Oracle 11R2 for testing RDBMS features
    * [EXOJCR-1408] - Improve the performance of the Property.setValue() methods
    * [EXOJCR-1409] - Testing RDBMS features on PostgreSQL (2 DBs)
    * [EXOJCR-1410] - Testing RDBMS features on MySQL (2 DBs)
    * [EXOJCR-1420] - Remove repository in Standalone mode
    * [EXOJCR-1422] - Remove repository in cluster mode
    * [EXOJCR-1423] - Remove reference from InitialContextBinder
    * [EXOJCR-1424] - Remove datasource from JNDI
    * [EXOJCR-1425] - Close all database connections
    * [EXOJCR-1429] - Improve the perfomance of NodeLockTest and NodeUnlockTest
    * [EXOJCR-1443] - Review tests in jcr-ext project


Changes of 1.14.0-CR3
====================

Bug
    * [EXOJCR-1088] - Extra exception stack trace[EXOJCR-1088] - Extra exception stack trace[EXOJCR-958] - Problem with WebDav on Jboss-server
    * [EXOJCR-1088] - Extra exception stack traces while browsing with Mac OS Finder through WebDAV
    * [EXOJCR-1123] - TestRollbackBigFiles.java failed on MySQL
    * [EXOJCR-1124] - Dolphin issues when moving resources/collections
    * [EXOJCR-1163] - RPCService is held when try to execute remote command inside other
    * [EXOJCR-1167] - Backup tests failed on Windows 7
    * [EXOJCR-1168] - HTTPBackupAgentTest failed on Windows 7
    * [EXOJCR-1178] - Incorrect MOVE method response when moving nodes between workspaces
    * [EXOJCR-1187] - Increase memory consuming of JCR, OutOfMemoryError: PermGen space
    * [EXOJCR-1200] - PROBLEMS during load testing *WARN * [Thread-72] LazyTextExtractorField: Exception reading value for field: Stream closed
    * [EXOJCR-1249] - FUNCTIONAL testing: test TestLockPerstistentDataManager is in error
    * [EXOJCR-1250] - FUNCTIONAL testing: on configuration multi, cache is turned off there're tests in error and in failure
    * [EXOJCR-1255] - TESTING: during load testing appear "The Network Adapter could not establish the connection" exception
    * [EXOJCR-1285] - Can't open versions of file with non-latin name
    * [EXOJCR-1305] - Orderable child nodes not honouring the order before method after a move
    * [EXOJCR-1319] - Still check lock on parent while isDeep = false
    * [EXOJCR-1320] - Case sensitivityProblem with Oracle Virtual Directory and SQL Server
    * [EXOJCR-1321] - TESTING: Can't open file with non-latin name via openoffice plugin. ErorCode: 404
    * [EXOJCR-1322] - MANUAL TESTING: problems during move folder on "client-server"
    * [EXOJCR-1327] - Need trigger events by default for Workspace.move()
    * [EXOJCR-1331] - Cache can contain NullNodeData for root node after RDBMS restore
    * [EXOJCR-1334] - Exception when edit the properties of a nt:resource node
    * [EXOJCR-1342] - Thread not stopped when the application is stopped
    * [EXOJCR-1344] - No eviction policy is allowed in case of the cache for indexing
    * [EXOJCR-1345] - Cluster coordinator change throws Timed out waiting for flush to unblock
    * [EXOJCR-1349] - Duplicate content of other workspaces in default workspace
    * [EXOJCR-1350] - Impossible to move files in Webdav when the destination path contains space (Windows)
    * [EXOJCR-1353] - NPE during incremental restore
    * [EXOJCR-1354] - BackupManagerImpl and MultiIndex doesn't suspend theirs threads on stop methods
    * [EXOJCR-1356] - Find a way to have a name for MBeans of JBossCaches used by the JCR 
    * [EXOJCR-1359] - PROBLEMS during functional testing: test TestUserTransaction is in failure
    * [EXOJCR-1360] - PROBLEMS during functional testing: test TestQueryUsecases is in failure
    * [EXOJCR-1367] - PROBLEMS during functional testing: test TestXATransaction is in error
    * [EXOJCR-1369] - PROBLEMS during functional testing: test NodeReadMethodsTest is in error
    * [EXOJCR-1371] - Properties can lose their "isMultivalued" flag after backup/restore operation.

Enhancement
    * [EXOJCR-1318] - Return only exptected attributes in SimpleLdapUserListAccess
    * [EXOJCR-1323] - Make the StandaloneContainerInitializedListener work with a configured relative path
    * [EXOJCR-1324] - Misspelling in some class names
    * [EXOJCR-1326] - Do not create new instance of MimeTypeResolver for each PUT request.
    * [EXOJCR-1337] - Do not check MultiIndex.checkIndexingQueue for finished documents
    * [EXOJCR-1338] - Remove unnecessary component from configuration to avoid failed builds on Jenkins
    * [EXOJCR-1343] - Avoid data accesses in case of new created nodes
    * [EXOJCR-1347] - Clean the code of the IndexerChangesFilters
    * [EXOJCR-1351] - Allow to manage the JCR Cache from JMX
    * [EXOJCR-1352] - Set check-sns-new-connection into false by default
    * [EXOJCR-1355] - Allow to suspend and resume the JobSchedulerService thanks to JMX and/or Rest
    * [EXOJCR-1357] - Add to the documentation the name of the existing categories of statistics

Feature Request
    * [EXOJCR-480] - Managed transactions support
    * [EXOJCR-1103] - Improve the methods Node.getProperties(String namePattern) and Node.getNodes(String namePattern)
    * [EXOJCR-1111] - Reduce the total amount of queries needed to update a property
    * [EXOJCR-1189] - Upgrade to use Tomcat AS v6.0.32
    * [EXOJCR-1325] - Make possible to specify custom mimetypes.properties for MimeTypeResolver
    * [EXOJCR-1328] - JCA support
    * [EXOJCR-1329] - Allow to use TransactionsEssentials as Transaction Manager
    * [EXOJCR-1333] - Provide a hasProfile method on ExoContainer

Task
    * [EXOJCR-769] - Port the RESTEndPoint annotation processor in WS sub-project
    * [EXOJCR-944] - WS : Do not generate files in src directory of a project
    * [EXOJCR-945] - JCR : Cleanup build
    * [EXOJCR-1009] - Check if membership type is present before membership is created
    * [EXOJCR-1221] - Resolve all the violations found by sonar when it is possible
    * [EXOJCR-1297] - Apply the patch file to JCR doc of PLF trunk for codes to be highlighted
    * [EXOJCR-1317] - Generate checksum for each artifact in project
    * [EXOJCR-1340] - [DOC]Add isolation level notice to the documentation
    * [EXOJCR-1348] - Use the DefaultChangesFilter in the configuration in case of a local mode
    * [EXOJCR-1358] - In WebDAV Interpret NoSuchWorkspaceException as HTTP CONFLICT state
    * [EXOJCR-1366] - minConnection and maxConnection params in the ldap configuration are not used
    * [EXOJCR-1372] - Align dependencies with EAP 5.1
    * [EXOJCR-1373] - Core.PDFDocumentReader support metadata UTF-16 encoding

Sub-task
    * [EXOJCR-1048] - TCK FrozenNodeTest has fails and exceptions - fix it
    * [EXOJCR-1049] - Check MultiConfigServiceTest
    * [EXOJCR-1050] - Check CommandServiceTest
    * [EXOJCR-1335] - Prepare benchmark tests
    * [EXOJCR-1363] - Investigate the reason of decreasing Session.getRootNode() performance
    * [EXOJCR-1364] - Review tests in jcr-core projects [part 1]
    * [EXOJCR-1365] - Appy patches and check the performance

Changes of 1.14.0-CR2
====================

Bug
    * [EXOJCR-1027] - TestPersistedValueData sometimes failed on hudson
    * [EXOJCR-1081] - TestBackupManager sometimes failed on hudson
    * [EXOJCR-1125] - FileCleaner is null in SysViewWorkspaceInitializer and BackupWorkapceInitializer.
    * [EXOJCR-1192] - FUNCTIONAL testing: test TestQueryUsecases is in failure
    * [EXOJCR-1234] - Orderable child node not honouring the add method
    * [EXOJCR-1258] - Backup console doesn't work with Platform 3.5
    * [EXOJCR-1263] - Problem of webdav on windows 7
    * [EXOJCR-1267] - Dialog window "About" has incorrect version of jcr in webdav ms office plugin
    * [EXOJCR-1270] - Workspace initializer is possible pushing an NullItemData into the cache when performing isWorkspaceInitialized() check
    * [EXOJCR-1282] - File with non-latin name can't be open: Can't open remote file.ErrorCode:500
    * [EXOJCR-1284] - Can't access file containing special characters in file name via Webdav
    * [EXOJCR-1290] - Problems with dispalying non-latin names (ar,fr,ua,ru,vn) after restore in backup
    * [EXOJCR-1291] - Component should be resumed in reverse order
    * [EXOJCR-1296] - Problem when renaming a large folder containing multiple files in webdav (Windows)
    * [EXOJCR-1300] - Use QueryParam to parameter "backup-set-path" in methods of HTTPBackupAgent
    * [EXOJCR-1307] - Failing tests in the webdav project due to an encoding issue
    * [EXOJCR-1308] - FUNCTIONAL testing: test TestJBossCacheWorkspaceStorageCacheInClusterMode is in failure
    * [EXOJCR-1310] - Error when get node definition for node

Component Upgrade
    * [EXOJCR-1279] - Upgrade ISPN to 4.2.1 FINAL

Enhancement
    * [EXOJCR-1233] - JCR configuration update require dropping JCR_CONFIG table
    * [EXOJCR-1241] - Give a more understandable name to all the existing Threads
    * [EXOJCR-1268] - Allow to rely on the ConfigurationManager to get the JGroups configuration when we use ISPN as underlying cache
    * [EXOJCR-1281] - Create database without datasource prefix in case of SingleDB
    * [EXOJCR-1286] - Very high response time when loading a page in the "Community Management" portlet when using ldap
    * [EXOJCR-1298] - Don't informative error at DefaultLoginModule.login() when the container is null.
    * [EXOJCR-1311] - Ensure possibility to restore without dump of lock tables
    * [EXOJCR-1314] - Improve QPath.isDescendantOf by using reverse order

Feature Request
    * [EXOJCR-577] - Allow to reindex a workspace asynchronously
    * [EXOJCR-1204] - Indexing failed with "Can't acquire lock timeout" exception
    * [EXOJCR-1206] - Create a data distribution service to help the applications to better distribute their child nodes
    * [EXOJCR-1269] - Allow to set a default value when we use variables in configuration files
    * [EXOJCR-1292] - Add possibility to know whether components are suspended or not
    * [EXOJCR-1294] - Allow to create sessions from ACLs
    * [EXOJCR-1301] - Allow to use external backup tools in a secure manner

Task
    * [EXOJCR-983] - Folders "logs" and "temp" are not on the same level as jetty
    * [EXOJCR-1019] - Ensure that we can connect to WebDAV server using digest authentication
    * [EXOJCR-1136] - Improve the NodeHierarchyCreator to better scale in term of users
    * [EXOJCR-1170] - Comparing RDBMS backup/restore/reindexing features with old implementation 
    * [EXOJCR-1229] - Write an Upgrade guide from 1.14 beta3 to 1.14 CR1
    * [EXOJCR-1257] - exobackup tool simplified command-line interface
    * [EXOJCR-1260] - Improve the Tomcat bundle to test all the existing implementations
    * [EXOJCR-1289] - Persisted data must be committed in cache without any transaction. 
    * [EXOJCR-1293] - Commons-lang dependency differs for JCR and Core
    * [EXOJCR-1302] - Data consistency - avoid possible data overwrite

Sub-task
    * [EXOJCR-1265] - Restore on MSSQL 2005 failed because of Connection reset.
    * [EXOJCR-1266] - RDBMS backup failed in cluster env with ISPN configuration
    * [EXOJCR-1271] - Check the length of the name of the cache for lock
    * [EXOJCR-1272] - The big batch commit in RDMBS restore on Oracle DB ws failed .
    * [EXOJCR-1273] - RDBMS restore failes on DB2 because of DB2 SQL Error: SQLCODE=-964, SQLSTATE=57011,
    * [EXOJCR-1274] - Check functional working RDBMS backup/restore feature with JBC
    * [EXOJCR-1275] - Ensure working on Windows OS
    * [EXOJCR-1276] - Determinate the way of comparing two configurations
    * [EXOJCR-1280] - Allow to reindex a repository/workspace thanks to JMX

Changes of 1.14.0-CR1
====================

Bug
    * [EXOJCR-1116] - RepositoryCreatorService stucks at RepositoryCreationSynchronizer.waitForApproval()
    * [EXOJCR-1122] - Location header missing in CREATED response for MOVE method
    * [EXOJCR-1179] - Concurrent eXo cache creation doesn't prevent multiple cache creation
    * [EXOJCR-1196] - ApplicationRegistry fails with NPE if any of javax.ws.rs.core.Application return null instead of empty collection of JAX-RS components
    * [EXOJCR-1197] - OrganizationService is started before NodeHierarchyCreator is initialized
    * [EXOJCR-1199] - Unprotected user code can stop container or org.picocontainer.Startable component
    * [EXOJCR-1201] - Fix WS packaging JS file for REST dependency
    * [EXOJCR-1210] - Restore from RDBMS backup fail on InnoDB(MySQL)
    * [EXOJCR-1213] - Tomcat doesn't stop after gatein.sh stop
    * [EXOJCR-1217] - Problem with the move function of webdav on https
    * [EXOJCR-1218] - Problem of renaming folders in WebDav
    * [EXOJCR-1232] - Unparseable variable in JCR XML configuration file
    * [EXOJCR-1235] - NFS stale handle
    * [EXOJCR-1238] - Not correct MBean components registration when PortalContainer contains more then one repository [Part2] 
    * [EXOJCR-1242] - UserImpl toString() wrong return value : only the @organizationId info.
    * [EXOJCR-1253] - PDFBox dependencies not complete in Core packaging

Enhancement
    * [EXOJCR-1149] - Load scriptPath resource with help of org.exoplatform.container.configuration.ConfigurationManagerImpl in org.exoplatform.services.database.creator.DBCreator
    * [EXOJCR-1182] - MimeTypeResolver does not work well with IE7
    * [EXOJCR-1185] - Reduce the time spent in ParentNodeEvictionActionPolicy
    * [EXOJCR-1188] - IndexInfos.write() should be called just after replaceIndexes operation done by IndexMerger
    * [EXOJCR-1231] - Help applications to prevent memory leaks by enabling the SessionCleaner by default
    * [EXOJCR-1240] - Give a more understandable name to all the existing Containers
    * [EXOJCR-1243] - JobSchedulerService interface should include: addPeriodJob and addCronJob
    * [EXOJCR-1254] - Add a mode that throws an Exception when we use a session that is not alive anymore

Feature Request
    * [EXOJCR-853] - Make it possible to create new repository in runtime from back-up in cluster environment
    * [EXOJCR-1078] - Implement a Backup/Restore Feature for RDBMS
    * [EXOJCR-1080] - Improve Lucene Indexing in a cluster environment
    * [EXOJCR-1089] - RootContainer configuration fails in JBossAS 6
    * [EXOJCR-1104] - Propose a re-indexing mechanism for RDBMS
    * [EXOJCR-1142] - Add application/x-jaxrs+groovy mime type to mimetypes.properties for *.grs extension
    * [EXOJCR-1160] - Glassfish Support
    * [EXOJCR-1177] - Improve JCR Doc
    * [EXOJCR-1180] - Allow to avoid using the cache factory under some specific conditions
    * [EXOJCR-1183] - Implement RequestLifeCycle in ResourceLauncher
    * [EXOJCR-1184] - Improve the re-indexing mechanism to take advantage of multi-cores
    * [EXOJCR-1202] - Support disable feature for ISPN cache
    * [EXOJCR-1207] - Export/Import child nodes version history in single xml with parent version history
    * [EXOJCR-1220] - Allow to use variables to define any values in the configuration file
    * [EXOJCR-1246] - StandaloneContainerInitializedListener should stop container on contextDestroyed method

Task
    * [EXOJCR-828] - JCR replication based on Infinispan
    * [EXOJCR-1112] - DBCleanerService: Support clean data for single workspace in case of multi-db
    * [EXOJCR-1157] - Upgrade to the latest maintenance version of Lucene 2.9
    * [EXOJCR-1158] - Review all the third party libraries
    * [EXOJCR-1159] - Upgrade all the choosen Third party libraries
    * [EXOJCR-1175] - Implement PDFDocumentReader.getProperties using PDFBox
    * [EXOJCR-1176] - Make text message of 405 (Method Not Allowed) status more meaningful
    * [EXOJCR-1208] - ConstraintViolationException when Importing Version history of a nt:folder node having a nt:file child node. Apply patch.
    * [EXOJCR-1230] - Update the js files in order to add the missing dependencies
    * [EXOJCR-1252] - Remove asm dependency from Kernel packaging

Sub-task
    * [EXOJCR-832] - JCR Indexer based on Infinispan
    * [EXOJCR-833] - Test the JCR Replication based on infinispan in non cluster mode
    * [EXOJCR-834] - Test the JCR Replication based on infinispan in cluster mode
    * [EXOJCR-930] - Testing in cluster environment
    * [EXOJCR-970] - Ensure the collocation of the data related to the same node when it is possible
    * [EXOJCR-1045] - Check the TestVersionRestore
    * [EXOJCR-1046] - Few excluded tests fails because of SecurityManager
    * [EXOJCR-1047] - Excluded TestSessionDataManager fails with NullPointerExceptions
    * [EXOJCR-1100] - Documentation and prepare testcases for QA
    * [EXOJCR-1153] - Support atomic restore
    * [EXOJCR-1174] - Each cluster node must have it's own index
    * [EXOJCR-1193] - Index retrieval from coordinator node
    * [EXOJCR-1214] - Adopt benchmark for quick ISPN <--> JBC switching. Set ISPN as default for benchmark and functional tests


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
* eXo Kernel 2.3.2-GA
* eXo Core 2.4.2-GA
* eXo WS 2.2.2-GA
* eXo JCR 1.14.2-GA

Find all 1.14 task on JIRA:
https://jira.jboss.org/jira/browse/EXOJCR

1.14.2-GA tasks:
https://issues.jboss.org/browse/EXOJCR/fixforversion/12317959

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

    * Copy exo.jcr.ear.ear and jcr-ds.xml into $jboss_home/server/default/deploy
    * Copy run.conf and run.conf.bato $jboss_home/bin
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
 