Summary

    Status: Backporting EXOJCR-1305 to JCR 1.12.x
    CCP Issue: CPP-915, Product Jira Issue: JCR-1613.
    Complexity: Low

The Proposal
Problem description

This bug happens randomly (like EXOJCR-1234) when a node is ordered after it was moved at the same parent (to rename it).
For instance let's suppose we have an ordered list ("a","b") and we want to rename "a" to "c" and keep the same order then the JCR operations to do it are:

session.move(parent.getPath() + "/a", parent.getPath() + "/c");
parent.orderBefore("c", "b");

Fix description

How is the problem fixed?

    New node is added to the end of the list of parent node as implementation says

Patch information:
Patch files: JCR-1613.patch

Tests to perform

Reproduction test
  * TestOrderBefore.java

Tests performed at DevLevel
  * Functional tests

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
  * No

Configuration changes

Configuration changes:
  * No

Will previous configuration continue to work?
  * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    No

Is there a performance risk/cost?
  * No

Validation (PM/Support/QA)

PM Comment
* Patch approved

Support Comment
* Patch validated

QA Feedbacks
*

