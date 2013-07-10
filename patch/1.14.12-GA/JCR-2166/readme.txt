Summary
	Issue title : ArrayIndexOutOfBoundsException when access webdav on Windows 7
	Product Jira Issue: JCR-2166.
	Complexity: N/A

Proposal

Problem description
What is the problem to fix?
	* ArrayIndexOutOfBoundsException when access webdav on Windows 7

Fix description
Problem analysis
	* There was a bug in retrieving empty multi-valued property. Instead of returning empty string, ArrayIndexOutOfBoundsException was thrown.

How is the problem fixed?
	* Return empty string if multi-valued property has no values

Tests to perform
Reproduction test
	* Start a clean server
	* Map a network drive to the repository collaboration
	* Go to Site content -> live -> acme
	* -> Result: After some seconds, throw out exception on terminal

Tests performed at DevLevel
	* Functional testing testPropFindWithEmptyMultiValuedProperty

Tests performed at Support Level
	* N/A

Tests performed at QA
	* N/A

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
	* N/A

Changes in Selenium scripts 
	* N/A

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

Any change in API (name, signature, annotation of a class/method)? No
Data (template, node type) upgrade:  No
Is there a performance risk/cost? No
