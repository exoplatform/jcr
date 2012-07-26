Summary
	* Issue title: Anonymous cannot access public nodes 
	* CCP Issue:  N/A 
	* Product Jira Issue: JCR-1832.
	* Complexity: middle

Proposal

Problem description

What is the problem to fix?
	* Anonymous cannot access public nodes 

Fix description

Problem analysis
	* Import returns parent permissions, but own permissions was already defined. 

How is the problem fixed?
	* The own permissions has been returned if it defined.

Tests to perform

Reproduction test
	# Access Content Explorer
	# Create folder A
	# Remove any permission of this folder
	# Upload a file B into folder A. B will not contain any permission because it inherited from A
	# Add any permission for file B.
	# Try to access file B by webdav link.
	# Exception appears in the console.

Tests performed at DevLevel
	* Manual testing, functional testing

Tests performed at Support Level
	* Reproduce test

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests

Changes in Selenium scripts 


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
	* Function or ClassName change: no
	* Data (template, node type) migration/upgrade: no

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
