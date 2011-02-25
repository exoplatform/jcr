Summary

    * Status: ConstraintViolationException when Importing Version history of an nt:folder node having an nt:file child node
    * CCP Issue: CCP-736, Product Jira Issue: JCR-1584
    * Fixes: ECMS-1903
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
ConstraintViolationException when Importing Version history of an nt:folder node having an nt:file child node.
Steps to reproduce:

   1. Create a web content named for example wc1
   2. Under wc1/medias/images add a node of type nt:file (upload an image for example IMG_0374.JPG)
   3. Create many versions of this document
   4. Export wc1 with its version history in the system view format
   5. Stop the server,clean the database and start the server again
   6. Import wc1 with its version history
      In console, there is a ConstraintViolationException.

Fix description

Problem analysis
The issue comes from the export data process, there is something wrong with the exported data:

    * Before exporting, the primary type of "IMG_0374.JPG" node is nt:file. After exporting, it becomes nt:versionedChild
    * In importing process, because nt:versionedChild node type is not allowed as child's node type for parent node type (nt:folder) so nodeTypeDataManager.isChildNodePrimaryTypeAllowed() function returns false and an ConstraintViolationException is thrown.

Root cause: SysViewImporter tests nt:versionedChild nodes whether it is allowed as child for nt:folder node. This is incorrect, since nt:versionedChild is allowed in any place below frozen node.

How is the problem fixed?

    * In case if exported node is nt:versionableChild and it is a descendant of frozen node - there is no child node primary type validation.

Patch information:
Patch files: JCR-1584.patch

Tests to perform

Reproduction test

    * Cf. above

Tests performed at DevLevel

    * Patch contains TestImportVersionedChild

Tests performed at QA/Support Level
*


Documentation changes

Documentation changes:
  * none


Configuration changes

Configuration changes:
  * none

Will previous configuration continue to work?
  * yes


Risks and impacts

Can this bug fix have any side effects on current client projects?

    * no

Is there a performance risk/cost?

    * Yes. There are additional validations at import. 

Validation (PM/Support/QA)

PM Comment
* Patch approved by the PM

Support Comment
* Patch validated

QA Feedbacks
*
Labels parameters

