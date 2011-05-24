Summary

    Status: Create testing repositories for version 1.12.x
    CCP Issue: JCR-1234, Product Jira Issue: JCR-1611.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?
We need to backport the fix of the jira EXOJCR-1234 to JCR 1.12.x.
Fix description

How is the problem fixed?

     Change the calculation way of last order number during node adding. The new order number will be as real last order number plus 1. In previous impl it was as children nodes count plus 1.

Patch information: JCR-1611.patch

Tests to perform

Reproduction test

* TestNodeOrder.java* *

Tests performed at DevLevel
  * functional testing in jcr core project

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
  * no

Configuration changes

Configuration changes:
  * No

Will previous configuration continue to work?
  * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    No

Is there a performance risk/cost?
  * Workspace.copy() method could be a bit slower. See tests results http://tests.exoplatform.org/JCR/1.12.9-GA/rev.4331_testukr_230/daily-performance-testing-results/jcr.core/index.html
Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

