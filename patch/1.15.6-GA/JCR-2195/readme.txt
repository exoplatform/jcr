Summary
	Issue title: Sybase 15.7 - error "Repository data is NOT consistent", error "expected:<588> but was:<654>"
	Product Jira Issue: JCR-2195.
	Complexity: N/A

Proposal

Problem description
What is the problem to fix?
	* Sybase 15.7 - error "Repository data is NOT consistent", error "expected:<588> but was:<654>"

Fix description
Problem analysis
	* We had duplicate values in the lucene indexes because the query used to retrieve the data during the indexing process could retrieve the same nodes from one thread to another because the query doesn't support offset.

How is the problem fixed?
	As the query doesn't support offset, the idea of the fix is to be able to indicate that offset is not supported by the database and if so we synchronize the data access to prevent getting several times the same nodes and then to prevent having several nodes in the lucene indexes
Tests to perform
Reproduction test
	* N/A

Tests performed at DevLevel
	I launched the unit tests mentioned by the JIRA issue which are TestRepositoryCheckController and TestQueryUsecases. Once the tests could pass, I launched the same unit test against MySQL PostgreSQL, Sybase, DB2, H2, HSQLDB and Oracle. Then I launched all the unit tests against sybase

Tests performed at Support Level
	* N/A

Tests performed at QA
	* N/A

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
	* No 

Changes in Selenium scripts 
	* No

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

Any change in API (name, signature, annotation of a class/method)? 
Data (template, node type) upgrade: 
Is there a performance risk/cost?
	* No
