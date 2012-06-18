Summary
	* Issue title: Search in cluster can return duplication of just modified nodes
	* CCP Issue:  CCP-1332 
	* Product Jira Issue: JCR-1727.
	* Complexity: High

Proposal
 
Problem description

What is the problem to fix?
	* Search in cluster returns duplication of just modified nodes

Fix description

Problem analysis
	* When some JCR contents are modified, their index is deleted and re-created.
	* Content is usually stored in persistent index on file system. Modifying it causes deletion of corresponding Lucene Document on file system and reindexing into the Volatile (in memory) index
	* But actually, Lucene Document is not deleted from persistent index, it is only marked as deleted in memory buffer. This greatly improve the performance, but causes issues in clustered environment
	* Thus non-coordinator cluster nodes doesn't see the fact that node deleted and query finds it in persistent and volatile indexes causing duplication.

How is the problem fixed?
	* Issue was fixed by applying deletions on non-coordinator nodes too, but in memory only. Those deletions are cleaned once coordinator flushes its buffers.

* Patch file: JCR-1727.patch

Tests to perform

Reproduction test
	* Scenario: Having a 2-nodes cluster setup
	* Steps to reproduce:
		- On one node
		- Go to Calendar
		- Create an event
		- Change in Month view
		- Choose the created event and try to move it
			--> Duplicate of selected event display on screen (see attached image)
	* Note:
		- Fress F5 to refresh the browser, the duplicate disappears.
		- The duplicate is displayed only on the calendar which has a move action. In the other calendar, the event has moved and haven't any duplicate.
		- Maybe it duplicates by the number of node that were used to test.


Tests performed at DevLevel
	* Manual testing in cluster of 3 nodes, unit tests.

Tests performed at Support Level
	* n/a 

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* none

Changes in Selenium scripts 
	*none

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
	* Function or ClassName change: internal JCR classes
	* Data (template, node type) migration/upgrade: none

Is there a performance risk/cost?
	* not spotted

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	* n/a
