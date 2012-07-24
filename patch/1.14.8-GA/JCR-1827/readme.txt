Summary

    * Issue title Cannot access jcr:content after importing data
    * Needed for KS-4511
    * CCP Issue:  N/A
    * Product Jira Issue: JCR-1827.
    * Complexity: middle

Proposal

 
Problem description

What is the problem to fix?

    * Import data into child node of exo:applications(which is restricted to administrator group)
    * The data to import contains set of nt:file nodes and they are all public node (acl: any read)
    * After they're imported successfully -> Normal user couldn't access those node although they have permission.

Exception in the log is:
javax.jcr.AccessDeniedException: Access denied []:1[]restricted:1[]accept.gif:1[http://www.jcp.org/jcr/1.0]content:1 for demo
    at org.exoplatform.services.jcr.impl.core.SessionDataManager.readItem(SessionDataManager.java:573)
    at org.exoplatform.services.jcr.impl.core.SessionDataManager.readItem(SessionDataManager.java:528)
    at org.exoplatform.services.jcr.impl.core.SessionDataManager.getItem(SessionDataManager.java:505)
    at org.exoplatform.services.jcr.impl.core.SessionImpl.getItem(SessionImpl.java:625)
...

Fix description

Problem analysis
* After importing, the node "/restricted/accept.gif/jcr:content" has the same permissions as node "/restricted", this is bug.
  It must have the same permissions same as node "/restricted/accept.gif".

How is the problem fixed?
    * Override method ImportNodeData.getACL(). This method is returning the true permission from property exo:privilege and return the true owner from property exo:owner if these properties present in node.   

Tests to perform

Reproduction test

    * Cf. above

Tests performed at DevLevel

    * 6 unit test were added in TestImport. 

Tests performed at Support Level

    * Cf. above

Tests performed at QA

    * no

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests

    * no

Changes in Selenium scripts 

    * no

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:

    * no

Configuration changes

Configuration changes:

    * no

Will previous configuration continue to work?

    * no

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: no
    * Data (template, node type) migration/upgrade: no

Is there a performance risk/cost?

    * no
