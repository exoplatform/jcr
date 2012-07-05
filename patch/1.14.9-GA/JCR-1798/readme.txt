Summary
	* Issue Title:Remove BackupChainLog from memory once the repository has been successfully created from backup 
	* CCP Issue:  N/A 
	* Product Jira Issue: JCR-1798.
	* Complexity: low

Proposal
 
Problem description

What is the problem to fix?
	* On cloud-ide.com after 740h of work, number of org.exoplatform.services.jcr.ext.backup.BackupChainLog instances is 4000. Total size of one of the lists in BackupManagerImpl 126Mb.

Fix description

Problem analysis
	* After restore, RepositoryBackupChainLog and BackupChainLog were stored in memory in JobRepositoryRestore.
	* After restore, JobRepositoryRestore was stored in memory, this need for getting status restore.
	* All JobRepositoryRestore was stored in memory after restore.

How is the problem fixed?
	* The BackupChainLog and RepositoryBackupChainLog are creating on demand in jobs for restore.
	* The mechanism for removing JopRepositoryRestore from memory was added.
	* This mechanism was used in RepositoryCreationService. RepositoryCreationService was used in Claud for creating repository from backup. 

Tests to perform

Reproduction test
	* No

Tests performed at DevLevel
	* Unit test was added for testing the mechanism for removing JopRepositoryRestore from memory after restore. 

Tests performed at Support Level
	* No

Tests performed at QA
	* No

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* No

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No
	* Data (template, node type) migration/upgrade: No 

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
