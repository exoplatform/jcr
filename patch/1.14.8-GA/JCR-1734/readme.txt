Summary
	* Issue title: Memory leak in operation Repository.stop
	* CCP Issue:  N/A
	* Product Jira Issue: JCR-1734.
	* Complexity: Medium

Proposal

 
Problem description

What is the problem to fix?
	* Memory leak in operation Repository.stop

Fix description

Problem analysis
	* Multiple references to Workspace-related object still remained after stopping the workspace.

How is the problem fixed?
	* Close all sessions on workspace.stop();
	* Properly remove JBoss Cache instances from internal maps of JBossCacheFactory when no more components use them;
	* Unregister Indexer when stopping workspace.
	* Take care of ISPN Cache instances. Properly instantiate them, remove from maps when no more components use them;
	* Clean map of Indexer-s in AbstractCacheStore on stop;
	* Release ISPN Cache instances when stopping the components using them;
	* Remove cache listener in IndexerCacheStore.
	* In addition fix for JCR-1721 integrated. Multiple RPC commands and listeners weren't unregistered and removed when stopping Workspace Container.


Tests to perform

Reproduction test
	* More than 1500 repositories created from backup stay infinite time in memory after Repository.stop

Tests performed at DevLevel
	* Manual profiling and added new unit test that reproduces the problem. Both standalone and clustered configuration checked.

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* Added new test case TestRepositoryManagement#testRepositoryContainerGCedAfterStop()

Changes in Selenium scripts 
	* none

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* none

Configuration changes

Configuration changes:
	* none

Will previous configuration continue to work?
	* yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change: internal JCR classes
    Data (template, node type) migration/upgrade: none

Is there a performance risk/cost?
	* none

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
