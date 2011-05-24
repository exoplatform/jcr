Summary

    Status: Problem of webdav on windows 7
    CCP Issue: CCP-823, Product Jira Issue: JCR-1597.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?
There are some problems when using webdav on Windows 7. We can't open a document (MS Office 2010) or the document is opened as read only (Open Office)
Fix description

How is the problem fixed?

     Lock tokens parsing fixed, to parse tokens not surrounded by '<' and '>' characters. To open documents via basic authentication on Windows 7 with MS Office 2010 we also need to edit registry.

Patch information:
Patch files: JCR-1597.patch

Tests to perform

Reproduction test
* Steps to reproduce:
1. Create a webdav drive on windows 7 by using this command net use o: "http://localhost:80/rest/private/jcr/repository/collaboration/" and those param must be verified:

    "web client" service must be turn on
    HKLM/system/currentversion/services/webclient/parameters/UseBasicAuth=2
    the port of the jboss has to be 80 for http and 443 for https, not 8080 and 8443
    2. Open the document

Tests performed at DevLevel

    Created WebDAV drive on Windows 7. Tested opening and saving documents with MS Office 2010 and OpenOffice.org 3.3.2.

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    Added description how to edit a registry to enable MS Office 2010 opening WebDAV documents using basic authentication over non-ssl connection.

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
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

