Summary
	* Issue title : Lucene exception on backup creation
    	* CCP Issue:  CCP-1332 
    	* Product Jira Issue: JCR-1746.
    	* Complexity: middle

Proposal

Problem description

What is the problem to fix?
	* Lucene exception on backup creation

Fix description

Problem analysis

How is the problem fixed?
	* The mechanism to wait for completion of all running query before suspend SearchIndex was added.


Tests to perform

Reproduction test
	* Performing backup creation on Platform 3.5.2 we face with Lucene exception thrown from JCR indexer internals.

Tests performed at DevLevel
	* The JUnit test for reproduce this problem was added.

Tests performed at Support Level

Tests performed at QA


Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* no

Changes in Selenium scripts 
	* no

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* no

Configuration changes

Configuration changes:
	* no

Will previous configuration continue to work?
	* no

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change: 
    Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?
	* no

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
