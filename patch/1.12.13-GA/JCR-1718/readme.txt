Summary
	* Issue Title: Query causes memory leak when fetching a large result set 
    	* CCP Issue:  CCP-1242 
    	* Product Jira Issue: JCR-1718.
    	* Complexity: Medium

Proposal
 
Problem description

What is the problem to fix?
	* Suppose doing a search which return a large result set(2000 items+) and I want to fetch the last items in the result by setting limit to 10 and offset to 1990. This job take me a very long time with low run of CPU/Memory.

Fix description

Problem analysis
	* Access permission is checked for all contents.

How is the problem fixed?
	* If "document-order" parameter is set to false in QueryHandler, access permission will not be checked

Patch file: JCR-1718.patch

Tests to perform

Reproduction test
	* No

Tests performed at DevLevel
	* Functional testing

Tests performed at Support Level
	*

Tests performed at QA
	*

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
	* Data (template, node type) migration/upgrade: No

Is there a performance risk/cost?
	* This fix improves performance because of skipping checking access permission if possible.

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
