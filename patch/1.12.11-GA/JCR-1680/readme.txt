Summary

    * Status: webdav bug when clicking go back link in a folder whose name contains a space
    * CCP Issue: CCP-1123, Product Jira Issue: JCR-1680.
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
How to reproduce on PLF 3.0.5:

1)Login
2)Type this URL in your browser http://localhost:8080/portal/rest/jcr/repository/collaboration/sites content/live/acme/web contents
3)Click on the ".." link
You'll be redirected to an erroneous link http://localhost:8080/portal/rest/jcr/repository/collaboration/sites content/live/acme/we
rather than being redirected to the parent folder.You'll get also an error message "Can't find path: /sites content/live/acme/we"
This happens with all folders whose name contains space character such as "web contents".

Fix description

How is the problem fixed?

    * Changed the way we determine the parent href for current collection. 
      Now we pass special attribute for it to the streaming output for the xslt insted of using the address of the current collection with the last element being cut off.

Patch file: JCR-1680.patch

Tests to perform

Reproduction test

    * Reproduced on PLF 3.0.5 while browsing collections which has 'space' character in the name

Tests performed at DevLevel

    * Reproduced on JCR WebDAV component. Added unit tests which covers this issue.

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
* Patch validated

Support Comment
* Patch validated

QA Feedbacks
*
