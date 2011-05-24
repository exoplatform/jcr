Summary

    Status: No eviction policy is allowed in case of the cache for indexing
    CCP Issue: N/A, Product Jira Issue: JCR-1629.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?
For indexing, JBC is used as the temporary/transactional/cluster aware memory such that if we add an eviction policy we could miss some nodes to index moreover using eviction algorithm such as FIFO add some contention.
Fix description

How is the problem fixed?

** *removing eviction policy from configuration

     

Patch information:
JCR-1629.patch

Tests to perform

Reproduction test
  * No

Tests performed at DevLevel
  * functional testing in jcr-core project

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
  * No

Configuration changes

Configuration changes:
  * Eviction policy is removed from cache index configuration

Will previous configuration continue to work?
  * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    No

Is there a performance risk/cost?
  * No

Validation (PM/Support/QA)

PM Comment
*

Support Comment
*

QA Feedbacks
*

