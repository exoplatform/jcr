Summary
	* Issue title: Method getSize of JBossCacheWorkspaceStorageCache disturbs cache eviction
	* CCP Issue:  N/A
	* Product Jira Issue: JCR-1751.
	* Complexity: Medium

Proposal

 
Problem description

What is the problem to fix?
	* Method getSize of JBossCacheWorkspaceStorageCache disturbs cache eviction

Fix description

Problem analysis
	* Traversing JBoss cache to calculate the number of alive nodes forces their refresh because of read access. It resets TTL of the nodes and their eviction is delayed. When calling getSize() more frequently than TTL of JBoss Cache entries, they will remain in memory causing a memory leak.

How is the problem fixed?
	* Problem was fixed by using internal JBoss Cache instruments that skips invocation of Interceptor Chain on read access and thus doesn't influence the eviction mechanism. Cache nodes are peeked from DataContainer directly without refreshing TTL timeouts.


Tests to perform

Reproduction test
	* Use PLF cache with the FIFO algorithm and a monitoring tool that polls getSize() on each workspace to avoid cache eviction

Tests performed at DevLevel
	* Manual and unit tests

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* none

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

    Function or ClassName change: none. Only internal JCR classes were updated.
    Data (template, node type) migration/upgrade: none.

Is there a performance risk/cost?
	* None. Possibly minimal unnoticeable improvement of getSize() method and some influence on top level project, because cache will work more efficiently now.

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
