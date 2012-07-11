Summary
	* Issue title: Remove backuped repository configuration after successfully save of new configuration in RepositoryServiceConfigurationImpl.retain 
	* CCP Issue:  N/A
	* Product Jira Issue: JCR-1797.
	* Complexity: Low

Proposal

Problem description

What is the problem to fix?
	* Remove backuped repository configuration after successfully save of new configuration in RepositoryServiceConfigurationImpl.retain 

Fix description

Problem analysis
	* Every RepositoryServiceConfigurationImpl adds one more file of JCR configuration. The total files number may exceed thousands.

How is the problem fixed?
	* Limit to total number of backup files


Tests to perform

Reproduction test
	* org.exoplatform.services.jcr.impl.core.TestRepositoryManagement.testBackupFilesRepositoryConfiguration

Tests performed at DevLevel
	* Functional testing at JCR core project

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	*

Changes in Selenium scripts 
	*

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* Documented "max-backup-files" value parameter usage.

Configuration changes

Configuration changes:
	* For RepositoryServiceConfigurationImpl have been added new parameter called "max-backup-files" . This option lets you specify the number of stored backups. Number of backups can't exceed this value. File which will exceed the limit will replace the oldest file.

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No
	* Data (template, node type) migration/upgrade:  No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
