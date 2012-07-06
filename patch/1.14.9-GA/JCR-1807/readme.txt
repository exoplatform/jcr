Summary
	- Issue title JTA: error during session.save() when global transaction marked for rollback
	- CCP Issue:  N/A
	- Product Jira Issue: JCR-1807.
	- Complexity: Low

Proposal

Problem description

What is the problem to fix?
	- JTA: error during session.save() when global transaction marked for rollback

Fix description

Problem analysis
	- JTA: error during session.save() when global transaction marked for rollback

How is the problem fixed?
	- Throw IllegalStateException if transaction is not in active state

Tests to perform

Reproduction test
	- Start JTA transaction
	- Obtain JCR session and create new node in JCR
	- Mark JTA transaction for rollback (tx.setRollbackOnly())
	- Call session.save()
 	-  Exception in server.log as JCR is trying to create new JTA transaction even there is already one active (It seems that checks for obtaining status of JTA transaction are failing as they count only with JTA transaction in state Status.ACTIVE)

Tests performed at DevLevel
	- functional testing

Tests performed at Support Level
	-

Tests performed at QA
	-

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	-

Changes in Selenium scripts 
	- 

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

	- Function or ClassName change:  No
	- Data (template, node type) migration/upgrade:  No

Is there a performance risk/cost?
	- No

Validation (PM/Support/QA)

PM Comment
	- Validated

Support Comment
	- Validated

QA Feedbacks
	- 
