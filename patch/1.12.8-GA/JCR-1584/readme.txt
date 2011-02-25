Summary

    * Status: ConstraintViolationException when Importing Version history of an nt:folder node having an nt:file child node
    * CCP Issue: CCP-736, Product Jira Issue: JCR-1584
    * Fixes: ECMS-1903
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * SysViewImporter tests nt:versionedChild nodes - does it allowed as child for nt:folder node. This is incorrevt, since nt:versionedChild allowed in any place below frozen node.
    * SystemViewStreamExporter exports pure nodes Version history. It is correct. But there is problem when we import version history of node, that have versionable subnode. And remove this node tree after export. In this case version history of versioned subnode will be lost.

Fix description

How is the problem fixed?

    * In case if exported node is nt:versionableChild and it is a descendant of frozen node - there is no child node primary type validation.
    * SystemViewStreamExporter exports versioned subnodes version history as sv:versionedChild structure into <sv:node>nt:verstionedChild</sv:node>
    * SysViewImporter imports all version histories in <sv:versionedChild> tags, and stores these version UUIDs in Context as "importedSubversions"-named list.
    * VersionHistoryImporter uses this list to update versionable child node properties (versionHistory, baseVerasion, predecessors) with actual values.

Patch information:
Patch files: JCR-1584.patch 	  	

Tests to perform

Reproduction test

    * Steps to reproduce:
      1)Create a web content named for example wc1
      2)Under wc1/medias/images add a node of type nt:file(upload an image for example IMG_0374.JPG)
      3)Create many version of this document
      3)Export wc1 with its version history in the system view format
      4)Stop the server,clean the database and start the server again
      5)Import wc1 with its version history
      In console, you will get a ConstraintViolationException

After investigation, the root cause comes from the export data process, there is something wrong with the exported data (the attached image shows it):

    * Before exporting, the primary type of "IMG_0374.JPG" node is nt:file. After exporting, it becomes nt:versionedChild
    * In importing process, because nt:versionedChild node type is not allowed as child's node type for parent node type (nt:folder) so nodeTypeDataManager.isChildNodePrimaryTypeAllowed() function returns false and an ConstraintViolationException is throwed

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

    * SysViewExporter class has new constructor with additional parameter.

Is there a performance risk/cost?

    * Yes. There are additional validations. Also export/import of big nodes tree with many versionable nodes may take more time.

Validation (PM/Support/QA)

PM Comment
* Patch approved by the PM

Support Comment
* Patch validated

QA Feedbacks
*

