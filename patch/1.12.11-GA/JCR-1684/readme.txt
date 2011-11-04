Summary

    * Status: exception when we import the same node twice with its version history
    * CCP Issue: CCP-1124, Product Jira Issue: JCR-1684.
    * Complexity: Medium

The Proposal
Problem description

What is the problem to fix?

    * Exception when we import the same node twice with its version history

Fix description

How is the problem fixed?

* Remove the version history of the node during import operation

Patch file: JCR-1684.patch

Tests to perform

Reproduction test
* TestImportVersionHistory

Tests performed at DevLevel
  * functional tests, manual testing on PLF 3.0.x

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
