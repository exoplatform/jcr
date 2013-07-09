Summary
	* Issue title: Swap file is not removed after adding property to the DB
	* Product Jira Issue: JCR-2051.
	* Complexity: N/A

Proposal

Problem description
What is the problem to fix?
	* Swap file is not removed after adding property to the DB

Fix description
Problem analysis
	* When the value storage is disabled and we set the value of a property using an InputStream, many files are created into the swap directory and they are never purged

How is the problem fixed?
	* We add a mechanism that remove the swap file as soon as the java object that refers to it is garbage collected and a mechanism that remove all the swap files of a workspace on startup

Tests to perform
Reproduction test
	* Run simple test with value storage disabled
	* In swap directory the file will be created.

Tests performed at DevLevel
	* org/exoplatform/services/jcr/impl/storage/jdbc/TestWriteOperations.java
we launch mvn clean install -P run-all,value-storage-disabled,cache-disabled and check that the swap directory is cleaned up regularly

Tests performed at Support Level
	* N/A

Tests performed at QA
Make sure that there is no perf and/or memory penalty
Changes in Test Referential: N/A
Changes in SNIFF/FUNC/REG tests: N/A

Changes in Selenium scripts 
	* None

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
Data (template, node type) upgrade: N/A
Is there a performance risk/cost?
	* No but is can be checked
