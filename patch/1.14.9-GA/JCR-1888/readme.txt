Summary
	* Issue title: Container repository was not started at the second startup in cluster environment with large dataset
    	* CCP Issue:  N/A
    	* Product Jira Issue: JCR-1888.
    	* Complexity: Low

Proposal
 
Problem description

What is the problem to fix?
	* Container repository was not started at the second startup in cluster enviroment with large dataset.

Fix description

Problem analysis
	* The problem do not happen during the first startup of the two nodes cluster.
	* In the second startup, We got the following stacktrace with second node and it is unusable.

How is the problem fixed?
	* Fixed file transferring with size exceeding 2G. Switching node offline just before creation initial index

Tests to perform

Reproduction test
	* Start cluster with large dataset twice.

Tests performed at DevLevel
	* functional testing

Tests performed at Support Level
	* Functional tests

Tests performed at QA
	* Tests with large dataset in cluster environment 

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
	* Data (template, node type) migration/upgrade:  No

Is there a performance risk/cost?
	*  No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
