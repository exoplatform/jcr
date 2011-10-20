Summary

    * Status: TESTING: Regression during daily testing
    * CCP Issue: N/A. Product Jira Issue: JCR-1661.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?
During daily testing we have regressions on some of tests, detail: http://tests.exoplatform.org/JCR/1.12.10-GA/rev.4778/daily-performance-testing-results/jcr.core/index.html

Fix description

How is the problem fixed?
    * For WorkspaceMoveTest and WorkspaceNodeUpdateTest: fixed by avoiding extra cache cleaning while running in cluster environment but with single node.
    * For other tests: fixed by avoiding extra putting NullItemData instances to cache.

Patch file: JCR-1661.patch

Tests to perform

Reproduction test

    *  Daily testing

Tests performed at DevLevel

    * Functional testing, daily testing

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:

    * None

Configuration changes

Configuration changes:

    * None

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
* Validated on behalf of Support.

QA Feedbacks
*
