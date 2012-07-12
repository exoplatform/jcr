Summary
	* Issue title: JbossCache in CacheableLockManagerImpl is not load data from cache loader after second start repository (stop and start on repository) in "jbosscache-shareable" mode.
	* CCP Issue:  CCP-1332 
	* Product Jira Issue: JCR-1731.
	*Complexity: Low

Proposal

 
Problem description

What is the problem to fix?
	* JbossCache in CacheableLockManagerImpl is not load data from cache loader after second start repository (stop and start on repository) in "jbosscache-shareable" mode.

Fix description

Problem analysis
	* ControllerCacheLoader allows data loading only as startup. Since several repositories can share same JBC instance after repository restarting we can not see locks in cache lock.

How is the problem fixed?
	* Forcing lock loading in cache at CacheableLockManagerImpl startup

Tests to perform

Reproduction test
* No

Tests performed at DevLevel
	* Functional testing, manual testing

Tests performed at Support Level

Tests performed at QA


Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	*

Changes in Selenium scripts 
	*

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
	* Function or ClassName change: 
	* Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
