Summary

    * Status: Invalid session.logout() calls in NodeHierarchyCreatorImpl
    * CCP Issue: CCP-1118, Product Jira Issue: JCR-1667.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?
In NodeHierarchyCreatorImpl some methods call session.logout before returning the JCR node which is totally invalid.
Fix description

How is the problem fixed?

    * session.logout method calls in NodeHierarchyCreatorImpl removed to have proper logic.

Patch file: JCR-1667.patch

Tests to perform

Reproduction test

    * Functional tests

Tests performed at DevLevel

    * Functional tests

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
* Patch validated

Support Comment
* Patch validated

QA Feedbacks
*
