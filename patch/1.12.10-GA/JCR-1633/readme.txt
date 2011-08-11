Summary

    * Status: Allow to keep missing values into the JCR Cache
    * CCP Issue: CCP-1032, Product Jira Issue: JCR-1633.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?

    * Allow to keep missing values into the JCR Cache.

Fix description

How is the problem fixed?

    * Keep NullNodeData and NullPropertyData in cache for missing values.

Patch file: JCR-1633.patch

Tests to perform

Reproduction test

    * When a node or a property value is missing, we keep nothing into the JCR cache so if an application on a top of the JCR tries to access several time to a missing data, the JCR will access the database each time. The idea will be to store the value "null" into the cache to prevent useless database accesses.

Tests performed at DevLevel
* No

Tests performed at QA/Support Level
* No

Documentation changes

Documentation changes:
* No

Configuration changes

Configuration changes:
* No

Will previous configuration continue to work?
* No

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change

Is there a performance risk/cost?
*
Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
*

QA Feedbacks
*

