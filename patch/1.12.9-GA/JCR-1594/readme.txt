Summary

    Status: Problem of renaming large folder in Webdav
    CCP Issue: CCP-812, Product Jira Issue: JCR-1594. Backport of JCR-1591.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?

    Renaming folder in WebDAV takes a lot of time.

Fix description

How is the problem fixed?

    Avoid unnecessary re-indexing operations for children nodes

Patch information:
Patch files: JCR-1594.patch

Tests to perform

Reproduction test

    Run Tomcat AS and mount WebDAV folder. Create folder and copy pdf-files (more than 200mb).  Try to rename folder, you can see that it takes a lot of time due to reindexing content.

Tests performed at DevLevel

    TCK tests, functional tests, manual testing on Tomcat AS

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    No 

Configuration changes

Configuration changes:
    No

Will previous configuration continue to work?
    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    No

Is there a performance risk/cost?
    No

Validation (PM/Support/QA)

PM Comment
* Patch validated by PM

Support Comment
* Support review: Patch validated

QA Feedbacks
*

