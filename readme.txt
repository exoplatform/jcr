JCR 1.12.0-Beta03 
=================


eXoPlatform Java Content Repository (JSR-170) implementation and Extension services.
Includes:
* eXo Kernel 2.2.0-Beta03
* eXo Core 2.3.0-Beta03
* eXo WS 2.1.0-Beta03


Release Notes - eXo-JCR - Version  JCR-1.12.0-Beta03

** Bug
    * [EXOJCR-176] - TestExcerpt fails in some cases
    * [EXOJCR-179] - tools.jar in the dependency tree (caused by htmlparser).
    * [EXOJCR-181] - Binary properties could be indexed differently in some cases
    * [EXOJCR-189] - Import of version history is not working well for nt:file
    * [EXOJCR-208] - Duplicated (ws-commons-util) entry in dependencyMangement
    * [EXOJCR-215] - JobSchedulerServiceImpl.addCronJob() ignores JobDataMap argument
    * [EXOJCR-232] - Jcr can store nodetype definitions with wrong isMultiple flag in some cases
    * [EXOJCR-239] - Update javax.servlet:servlet-api dependency to version 2.4 for project exo.ws.testframework.

** Feature Request
    * [EXOJCR-233] - Add possibility update entry in Registry or if not exist create it 

** Task
    * [EXOJCR-28] - "update-policy" parameter : Use the check-in/check-out only on versionned files
    * [EXOJCR-131] - Add D:getcontenttype and jcr:nodeType properties to SEARCH method response.
    * [EXOJCR-163] - [SWF] Maven best practice : module name = artifactId
    * [EXOJCR-165] - Maven convention : Move resources from [main|test]/java to [main|test]/resources
    * [EXOJCR-178] - Provide a mechanism to manage Cache-Control header value for different mime-types from server configuration.
    * [EXOJCR-183] - full-text method SEARCH does not find out text file
    * [EXOJCR-184] - 1.12-Beta02 performance profiling 
    * [EXOJCR-195] - Update Oracle script with analyze statements
    * [EXOJCR-196] - Remove META-INF folder from jcr.core src resources
    * [EXOJCR-206] - Add MimeType "application/x-groovy", "script/groovy", "application/x-javascript" to the TextPlainDocumentReader.
    * [EXOJCR-218] - Extend ItemExistsException in JCRItemExistsException with details about an error
    * [EXOJCR-219] -  Optimize creation of ChangesLog from full backup.
    * [EXOJCR-191] - Save default node types  in repository


Resources
=========

 Project site        	http://www.jboss.org/exojcr
 Company site        	http://www.exoplatform.com
 Documentation wiki   	http://wiki.exoplatform.org
 Community JIRA      	http://jira.exoplatform.org
 Comminity site      	http://www.exoplatform.org
 Community forum     	http://www.exoplatform.com/portal/public/en/forum			     
 JavaDoc site        	http://docs.exoplatform.org
 