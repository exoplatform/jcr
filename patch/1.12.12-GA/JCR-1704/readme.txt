Summary

    * Status: Portlet crash and no preview available after save from Office 2010 in webdav
    * CCP Issue: CCP-1182, Product Jira Issue: JCR-1704.
    * Complexity: medium

The Proposal
Problem description

What is the problem to fix?
Portlet crash and no preview available after save from Office 2010 in webdav
To reproduce this issue:

    * Create a webdav drive, for example "http://localhost:8080/rest/private/jcr/repository/collaboration/"
    * Copy any doc or DOCX created by MS Office 2010 to this drive
    * Access to the document in the Site Explorer => The icon associated to the MIME type of DOCX is the icon of nt file
    * Open the document in the webdav drive and make a modification (even one character) then save it.
    * Access again to Site Explorer
      => The MIME type and the icon of the document have changed to XML.
      => The portlet crashes and it is difficult to get the interface back.

Fix description

How is the problem fixed?

    * New WebDavService initial parameter added to contain a set of untrusted user agents. 
      Content-type headers sent by this user agents are now ignored, we use instead MimeTypeResolver to define resource's MIME type.

Patch file: JCR-1704.patch

Tests to perform

Reproduction test
1. Case 1: cf. above
2. Case 2: 
* Create a webdav drive, for example "http://localhost:8080/rest/private/jcr/repository/collaboration/"
* Create a new document by Office 2010 inside that web folder
* Save the document directly in the webdav folder (no copy from local)
  => The MIME type passes directly to xml before doing any modification in the document.

Tests performed at DevLevel
* Launch PLF to manually reproduce usecase.
* Creat unit test with the usecase.

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
* Added description and example of a new initial parameter for WebDavService

Configuration changes

Configuration changes:

    * Added new initial parameter for WebDavService

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Validated

Support Comment
* Validated

QA Feedbacks
*
