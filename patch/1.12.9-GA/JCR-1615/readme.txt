Summary

    Status: Problem when publishing a locked item
    CCP Issue: CCP-848, Product Jira Issue: JCR-1615.
    Fixes also: ECMS-2079
    Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
1- Go to the Drive "Collaboration"
2- Lock the "documents" folder
3- Create a new document in the "documents" folder, save it as draft than close it
4- Click on the "publish" button: we get an exception: "javax.jcr.lock.LockException: Node /Documents is locked..."
5- Changing publication state through Manage Publications action: it works fine even if the item is locked.
Fix description

How is the problem fixed?

    On JCR side locking bug is fixed. When calling Node.checkIn() JCR checks if parent is locked, that is actually a bug. Since Node.checkIn() doesn't modify the parent node, so it should only check if current node not locked.

Patch information:
Patch files: JCR-1615.patch

Tests to perform

Reproduction test

    org.exoplatform.services.jcr.api.lock.TestLock.testCheckInWhenParentLocked()

Tests performed at DevLevel

    Full set of JCR-TCK and eXo-JCR tests

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    No

Configuration changes

Configuration changes:
    No

Will previous configuration continue to work?
    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    None

Is there a performance risk/cost?
    No

Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
* Patch validated.

QA Feedbacks
*

