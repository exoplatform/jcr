Summary
	* Issue title: Temporary file are not removed if IOException occur during spooling of InputStream value
	* CCP Issue:  CCP-1332 
	* Product Jira Issue: JCR-1738.
	* Complexity: Low

Proposal
 
Problem description

What is the problem to fix?
	* Temporary file are not removed if IOException occur during spooling of InputStream value

Fix description

Problem analysis
	* Temporary spool file remains forever on FS after IOException

How is the problem fixed?
	*  By removal or adding file to FileCleanr

    Patch file: JCR-1738.patch

Tests to perform

Reproduction test
	* Save InputStream (aprox >100Mb)
	* During save (aprox after 11 mb) IOException occur
	* Part of the stream spooled as jcrvd file but not remove after IllegalStateException in org.exoplatform.services.jcr.impl.dataflow.TransientValueData.NewValueData.spoolInputStream()

Tests performed at DevLevel
	* Functional testing

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
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change:  no
	* Data (template, node type) migration/upgrade:  no

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
