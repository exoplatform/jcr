Summary

    Status: Delay in replication of Nodes data in JBoss EPP
    CCP Issue: CCP-984, Product Jira Issue: JCR-1639.
    Complexity: High

The Proposal
Problem description

What is the problem to fix?

    Delay in replication of Nodes data in JBoss EPP

Fix description

How is the problem fixed?

 * Need to rollback JDBC connection before closing. 

Patch information:
JCR-1639.patch

Tests to perform

Reproduction test

    A folder was created with the name 00480017, and a document inside it named 00480017-test1
    After few seconds, the folder just disappeared. We tried to access it using its full path /00480017 but, it wasn't there anymore. Message that the folder doesn't exist or something like that.
    Then, we tried to create a new folder with the name name of the previous, then we got a "Can not save. This node doesn't allow 2 nodes with the same name" message.
    After ~15 minutes, the folder appeared again.

Tests performed at DevLevel
  * Functional testing, manual testing in customer env.

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

    No

Is there a performance risk/cost?
  * it is possible a bit perf decreasion on MySQL with InnoDB engine. See QA reports (two last columns) http://tests.exoplatform.org/JCR/1.12.10-GA/rev.4553/daily-performance-testing-results_TESTUKR-260/jcr.core/index.html&nbsp;

Validation (PM/Support/QA)

PM Comment

    Patch approved by the PL

Support Comment

    Support review: Patch validated

QA Feedbacks
*

