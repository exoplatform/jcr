Summary

    * Status: Contention on JCRDateFormat on heavy load
    * CCP Issue: CCP-1118, Product Jira Issue: JCR-1663.
    * Complexity: low

The Proposal
Problem description

What is the problem to fix?
When we activate the Concurrent GC during our benchmark, we have very poor performances and we can observe that a huge amount of the http thread are waiting on the same piece of code in org.exoplatform.services.jcr.impl.util.JCRDateFormat.deserialize(String serString) due to the use of the synchronized method java.util.TimeZone.getTimeZone(String ID)

Expected: an alternative to avoid/limit the usage of this synchronized method.

Fix description

How is the problem fixed?

    * Avoided usage of method java.util.TimeZone.getTimeZone(String ID), now we use less synchronized method implemented in kernel project instead.

Patch file: JCR-1663.patch

Tests to perform

Reproduction test

    * Activate the Concurrent GC during run of benchmark

Tests performed at DevLevel

    * Functional testing for all projects (kernel, core, ws, jcr)

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
* PM validated

Support Comment
*

QA Feedbacks
* 

