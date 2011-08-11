Summary

    * Status: Performance issues due to SecureRandom.nextBytes under heavy load
    * CCP Issue: CCP-1032, Product Jira Issue: JCR-1654.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?
During the benchmarks we encounter a weird slowness off UUID generation in the system.
A detailed study has been performed and in short it occurs sometimes and for some OS the UUID method is really slow under heavy load which causes a huge bottleneck.

Fix description

How is the problem fixed?
    * Avoid UUID generation for id of SessionImpl. Now, id of Sessionimpl is generated as "System.currentTimeMillis() + "_" + SEQUENCE.incrementAndGet()"

Patch file: JCR-1654.patch

Tests to perform

Reproduction test
* No

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
*  Little increase performance under heavy load.

Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
*

QA Feedbacks
*
