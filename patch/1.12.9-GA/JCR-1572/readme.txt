Summary

    Status: Improve the re-indexing mechanism to take advantage of multi-cores
    CCP Issue: N/A, Product Jira Issue: JCR-1572.
    Complexity: Medium

The Proposal
Problem description

What is the problem to fix?
Tested EPP 5.0.1 with 19 000 users created thanks to crash and its advanced command addusers that you can found here https://github.com/exoplatform/gatein-stuff. The table JCR_SITEM contains 4,3 Millions of rows and JCR_SVALUE contains 3,2 Millions of rows, in this case if I try to re-index my content that contains 1 216 000 nodes, the re-indexing takes 3 h 40 minutes on my laptop which is a dual core. The idea of my patch proposal is to propose a multi-threaded re-indexing mechanism in order to take advantage of multi-cores which is commonly used now, thanks to this patch the re-indexing takes 2 h 20 minutes on my laptop knowing that the main bottelneck is the db.

Please check the patch and if it is ok ask the QA to load a big table and re-index it with and without the patch to see the gain in their environment.
Fix description

How is the problem fixed?

* involve several threads in jcr indexing operation* *

Patch information:
Patch files: JCR-1572.patch

Tests to perform

Reproduction test
* No

Tests performed at DevLevel
* functional testing in JCR project

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
* No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

