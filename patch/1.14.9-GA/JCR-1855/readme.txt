Summary
	* Issue title:  New content in several templates created by CE is converted to text/plain after editing by webdav
	* CCP Issue:  N/A
	* Product Jira Issue: JCR-1855.
	* Complexity: low

Proposal
 
Problem description

What is the problem to fix?
	* New content in several templates created by CE is converted to text/plain after editing by webdav

Fix description

Problem analysis
	* If we PUT via WebDAV a file with no extension, the mime-type of the file may be resolved incorrectly and thus incorrectly overridden.

How is the problem fixed?
	* Need to know if the mime-type is resolved incorrectly and do not allow to override the mime-type property.

Tests to perform

Reproduction test
	* In PLF server
	* Create a content with HTML template named tata. It's mimetype is text/html.
	* Edit it by Webdav then save it -> Its mimetype is automatically converted to text/plain.

Tests performed at DevLevel
	* Unit tests covering the situation when we PUT a file w/o extension and other situations where we cannot resolve mime-type correctly

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
	* None

Configuration changes

Configuration changes:
	* None

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: no
	* Data (template, node type) migration/upgrade: no

Is there a performance risk/cost?
	* no

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
