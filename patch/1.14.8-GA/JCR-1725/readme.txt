Summary
	- Issue title: isSuspended filed in CacheableWSDataManager, SeachManager and SearchIndex must be AtomicBoolean, index merger must be disposed after flush on close
	- CCP Issue:  CCP-1332 
	- Product Jira Issue: JCR-1725.
	- Complexity: N/A

Proposal
 
Problem description

What is the problem to fix?
	- Sometimes MultiIndex can cause NPE exceptions when calling close while some write operations takes place. In addition need to ensure Thread-safeness for Index components and CacheableWorkspaceDataManager.

Fix description

Problem analysis
	- When calling MultiIndex.close with non empty volatile index, it invokes flush() to store changes on file system. But Merger already disposed and this causes exception. In addition, some flags and fields in Index and CacheableWorkspaceDataManager can be accessed within multiple threads but they doesn't guarantee thread safeness.

How is the problem fixed?
	- Merger is disposed after calling flush() in MultiIndex.close() now and AtomicBoolean and AtomicReference objects are used to ensure Thread-safeness in mentioned components of JCR. 

Patch file: JCR-1725.patch

Tests to perform

Reproduction test
	- isSuspended filed in CacheableWSDataManager, SeachManager and SearchIndex must be AtomicBoolean, index merger must be disposed after flush on close

Tests performed at DevLevel
	- Unit tests.

Tests performed at Support Level
	- n/a

Tests performed at QA
	-

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	- None

Changes in Selenium scripts 
	- None

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	- None

Configuration changes

Configuration changes:
	- None

Will previous configuration continue to work?
	- Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	- Function or ClassName change: Internal components and classes
	- Data (template, node type) migration/upgrade:  none

Is there a performance risk/cost?
	- None

Validation (PM/Support/QA)

PM Comment
	- Validated

Support Comment
	- n/a

QA Feedbacks
	- n/a

