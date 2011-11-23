Summary

    * Status: Propose a maven profile for each supported configuration
    * CCP Issue: N/A, Product Jira Issue: JCR-1689.
    * Depends on KER-180
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
To allow the QA to launch the functional tests on each supported config composed of DB Type/DB Version/Driver Version we need to add the remaining profiles in order to have the full list.

Fix description

How is the problem fixed?
 * Add new profiles to run functional tests on all supported DBs

Patch file: JCR-1689.patch

Tests to perform

Reproduction test

    * No

Tests performed at DevLevel

    * No

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
* Validated by PM on behalf of Support

QA Feedbacks
*
