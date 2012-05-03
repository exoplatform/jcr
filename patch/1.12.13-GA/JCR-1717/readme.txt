Summary

	* Issue title: Ensure that all the running transactions are over before allowing to stop the JCR
	* CCP Issue: N/A
	* Product Jira Issue: JCR-1717.
	* Complexity: Medium

Proposal
 
Problem description

What is the problem to fix?
	* Ensure that all the running transactions are over before allowing to stop the JCR

Fix description

Problem analysis
	* When repository is stopping db, index or value storage can be in inconsistency state

How is the problem fixed?
	* Allow to end all currently run transactions and only then continue repository stopping 

Tests to perform

Reproduction test
	* Ensure that if when we have a JCR running with updates in progress and the administrator calls Ctrl + C to properly stop the server, we need to be sure that the running transactions are over before stopping the JCR otherwise it could create some inconsistency between for example the JCR data and the Lock Data and/or the luncene indexes

Tests performed at DevLevel
	* Manual testing

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential
	* No

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
	* Function or ClassName change: N/A 
	* Data (template, node type) migration/upgrade: No 

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	* 
