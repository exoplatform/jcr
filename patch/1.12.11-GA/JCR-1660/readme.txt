Summary

    * Status: Problems during testing of Lock operations (EditLockedCommonNodeTest, EditLockedCommonDeepNodeTest)
    * CCP Issue: N/A. Product Jira Issue: JCR-1660.
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
During lock tests (EditLockedCommonNodeTest, EditLockedCommonDeepNodeTest), there are warnings and exceptions. 

Fix description

How is the problem fixed?
* Check whether the parent node is locked or not.
* Then try to check whether the property exists or not.

Patch file: JCR-1660.patch

Tests to perform

Reproduction test
  * Weekly tests (EditLockedCommonNodeTest, EditLockedCommonDeepNodeTest)

Tests performed at DevLevel
  * functional testing jcr-core project

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

    * No

Is there a performance risk/cost?
  * No

Validation (PM/Support/QA)

PM Comment
* PM validated

Support Comment
* Support validated

QA Feedbacks
*

