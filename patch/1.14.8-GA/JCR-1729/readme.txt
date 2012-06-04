Summary
	- Issue title Remove workaround about using ConcurrentHashMap
	- CCP Issue:  CCP-1332 
	- Product Jira Issue: JCR-1729.
	- Complexity: Low

Proposal
 
Problem description

What is the problem to fix?
	- Remove workaround about using ConcurrentHashMap

Fix description

Problem analysis
	- ConcurrentHashMap is caused the ConcurrentModficationException 

How is the problem fixed?
	- Using iterator for removing items and replacing ConcurrentHashMap by HashMap
	
	Patch file: JCR-1729.patch

Tests to perform

Reproduction test
	- n/a

Tests performed at DevLevel
	- Functional testing

Tests performed at Support Level
	- n/a

Tests performed at QA
	- n/a

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	- n/a

Changes in Selenium scripts 
	- n/a

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	- No

Configuration changes

Configuration changes:
	- No

Will previous configuration continue to work?
	- Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	- Function or ClassName change: no
	- Data (template, node type) migration/upgrade:  no

Is there a performance risk/cost?
	- no

Validation (PM/Support/QA)

PM Comment
	- Patch Validated

Support Comment
	- n/a

QA Feedbacks
	- n/a


