Summary
	* Issue title: Performance issues met with MySQL InnoDB
	* CCP Issue:  CCP-1296
	* Product Jira Issue: JCR-1742.
	* Complexity: Low

Proposal
 
Problem description

What is the problem to fix?
	* Performance issues met with MySQL InnoDB

Fix description

Problem analysis
	* PLF with MySQL InnoDB starts very slowly.

How is the problem fixed?
	* Fix contains two improvements: forced index for query and rewrote query for table checking existing.

Tests to perform

Reproduction test
	* Performance is down with MySQL InnoDB. Starts PLF on MySQL InnoDB with 5000 users (with gathering statistics). You can see that start takes a lot of time. Apply patch and restart PLF.
	* Starting time is improved and statistics for getItemById query is improved too.

Tests performed at DevLevel
	* Functional tests, manual tests on PLF

Tests performed at Support Level
	* n/a

Tests performed at QA
	* n/a

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* n/a

Changes in Selenium scripts 
	* n/a

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
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
