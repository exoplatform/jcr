Summary
	* Issue title PrimaryTypeNotFoundException when repairing data 
	* CCP Issue:  CCP-1451 
	* Product Jira Issue: JCR-1853.
	* Complexity: Medium

Proposal

Problem description

What is the problem to fix?
	* PrimaryTypeNotFoundException when repairing data 

Fix description

Problem analysis
	* Repairing tree where every node has no primaryType property need to use SQL query instead of using WorkspaceStorageConnection.delete(NodeData data) method

How is the problem fixed?
	* Refactoring NodeRemover class. 

Tests to perform

Reproduction test
	* TestRepositoryCheckController.testDBUsecasesTreeOfNodeHasNoProperties

Tests performed at DevLevel
	* Functional testing

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
	* NO

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change:  No
	* Data (template, node type) migration/upgrade:  No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

