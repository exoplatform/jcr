Summary

    Status: JCR addNode within a transaction causes javax.transaction.HeuristicMixedException in the first access to the node
    CCP Issue: CCP-870, Product Jira Issue: JCR-1604.
    Complexity: High

The Proposal
Problem description

What is the problem to fix?

     JCR addNode within a transaction causes javax.transaction.HeuristicMixedException (internally, org.exoplatform.services.transaction.TransactionException caused by org.jboss.cache.lock.TimeoutException) in the first access to the node

Fix description

How is the problem fixed?

    Perform loading data in cache outside the current transaction

Patch information: JCR-1604.patch

Tests to perform

Reproduction test
* Steps to reproduce:

    Copy test-jcr.bsh to $JBOSS_HOME/server/$PROFILE/deploy
    Start EPP (during the startup sequence, test-jcr.bsh will be executed once first=true)
    Edit the test-jcr.bsh to set first=false and save (Then redeploy will be triggered and test-jcr.bsh will be executed again first=false)
    Reboot EPP (during the startup sequence, test-jcr.bsh will be executed first=false)

Tests performed at DevLevel
* Do the same tasks like reproduction test => the scroll bar appears

Tests performed at QA/Support Level
*


Documentation changes

Documentation changes:
  * No


Configuration changes

Configuration changes:
  * No

Will previous configuration continue to work?
*Yes


Risks and impacts

Can this bug fix have any side effects on current client projects?

    No

Is there a performance risk/cost?
  * No

Validation (PM/Support/QA)

PM Comment

*Patch approved by the PM

Support Comment
*Support patch review: patch tested and approved

QA Feedbacks
*

