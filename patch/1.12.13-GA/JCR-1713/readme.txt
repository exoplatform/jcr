Summary
	* Issue title: NullPointerException on trying to unlock content 
	* CCP Issue:  CCP-1234
	* Product Jira Issue: JCR-1713.
	* Complexity: High

Proposal
 
Problem description

What is the problem to fix?
	* NullPointerException on trying to unlock content 

Fix description

Problem analysis
	* Cache locks and database locks were resynchronized.

How is the problem fixed?
	* Splitting commit into 2 phases. Also a special JDBC cache loader has been created for JBC in order to  be able to do commit and rollback on the value inserted into db.

    Patch file: JCR-1713.patch

Tests to perform

Reproduction test
	* The content in CMS sometime gets locked by a user and that user isn't able to unlock it then and even through Admin console's "Manage locks". The exception is fired and not able to unlock it

Tests performed at DevLevel
	* Functional testing + manual testing

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
	* Mentioning about db engine is transactional, using  org.exoplatform.services.jcr.impl.core.lock.jbosscache.JDBCCacheLoader for configuration of cache locks.

Configuration changes

Configuration changes:
	* org.exoplatform.services.jcr.impl.core.lock.jbosscache.JDBCCacheLoader should be used CacheLoader for cache locks.

Will previous configuration continue to work?
	* yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change: N/A
    Data (template, node type) migration/upgrade: No 

Is there a performance risk/cost?
	* No, performance testing were done without regression

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
