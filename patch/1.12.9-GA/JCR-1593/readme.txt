Summary

    Status: Problem with the move function of webdav on https
    CCP Issue: CCP-799, Product Jira Issue: JCR-1593. Backport of JCR-1588.
    Complexity: low

The Proposal
Problem description

What is the problem to fix?

    Using webdav with https, it's impossible to rename or move a file or directory. The applied Architecture is an Apache in front which manages the https and AJP connectors that transfer requests to the server JBoss

Fix description

Problem analysis
This error is caused by the following line of code in WebDavServiceImpl.java:

destinationHeader = serverURI + escapePath(destinationHeader.substring(serverURI.length()));

The output of this line is https://localhost:443/rest/private/jcr/repositorylaboration/ so the workspace collaboration is not found. The consideration of the port 443 is badly done (By using Apache, we don't specify the port).
The path obtained by this line of code:

destinationHeader = TextUtil.unescape(destinationHeader, '%'); is correct (without the specification of the port 443)

There is no problem with http.

How is the problem fixed?

    Change the algorithm of parsing destination URI and base URI. Now we use java.net.URI which itself does all the parsing.

Patch information:

    Final files to use should be attached to this page (Jira is for the discussion)

Patch files:
There are currently no attachments on this page.
Tests to perform

Reproduction test
* Steps to reproduce:
1. Create a webfolder using https:https://localhost/rest/private/jcr/repository/collaboration/
2. In this webfolder, create or upload a repository
3. Try to rename the directory or file. => An error

Tests performed at DevLevel

    Tested MOVE methods for move and rename operations with Dolphin and Nautilus webdav clients.

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    None

Configuration changes

Configuration changes:
    None

Will previous configuration continue to work?
    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    No

Is there a performance risk/cost?
    No

Validation (PM/Support/QA)

PM Comment
* Patch approved

Support Comment
* Patch validated

QA Feedbacks
*

