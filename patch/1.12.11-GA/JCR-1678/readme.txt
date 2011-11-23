Summary

    * Status: Bad performance in ChildAxisQuery.SimpleChildrenCalculator.getHits()
    * CCP Issue: N/A. Product Jira Issue: JCR-1678.
    * Complexity: High

The Proposal
Problem description

What is the problem to fix?
Using a dataset with 20k users, the Calendar home page takes 160s to display the first time, and 3/4s later.
After 1h (JCR Cache eviction), it takes also 160s to display.
The problem doesn't occur much with 10k users (7s to display).

Fix description

How is the problem fixed?

* Avoid invoking getChildNodesData() by storing some needed information in lucene index.
  Need to re-index data to ensure that new changes will work.

Patch file: JCR-1678.patch

Tests to perform

Reproduction test
  * No

Tests performed at DevLevel
  * Functional tests

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
*

Support Comment
*

QA Feedbacks
*
