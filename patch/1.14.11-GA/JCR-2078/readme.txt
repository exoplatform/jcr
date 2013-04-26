Summary
	* Issue title: Propagation of permission from parent to children
	* Product Jira Issue: JCR-2078.
	* Complexity: N/A

Proposal

Problem description
What is the problem to fix?
	* In ECMS-4591, when we update the permission of parent's node, this permission is not propagated to the children. This should be fixed in JCR code.
	* The issue JCR-1117 is related and I can still reproduce JCR-1117 in PLF 3.5.6.

Fix description
Problem analysis
	* On permission change as the sub node was already "exo:owneable", the JCR skipped the change in the JCR Cache on the sub node and its descendants.
How is the problem fixed?
	* Now the JCR only skips permission changes in the JCR cache if and only if the sub node has both "exo:owneable" and "exo:privilidgeable" or if it has one of them and the new ACL built from what is inherited from the parent node and what we have at node level is different from the old ACL

Tests to perform

Reproduction test
	* In ECMS, login as john and go to acme > documents
	* Create folder parent and its children folder child1, child2
	* child1: add permission for james
	* parent: delete any permission
	* Select to view the permission of child1 node. This node has any permission and james's permission => OK
	* Select to view the permission of child2 node. This child2 node still has any permission => NOK
	* Problem: The permissions of the parent is not propagated to the children
	* Expect behavior: when modifying parent's node permission, this permission is propagated to all of its children which didn't modify permission yet.

Tests performed at DevLevel
	* New test methods have been added in the Testcase TestPermissions

Tests performed at Support Level
	* Reproduction test

Tests performed at QA
	* N/A

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
	* N/A
Changes in Selenium scripts 
	* N/A

Documentation changes
Documentation (User/Admin/Dev/Ref) changes:
	* None

Configuration changes
Configuration changes:
	* None

Will previous configuration continue to work?
	* Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?
	* Any change in API (name, signature, annotation of a class/method)? 
	* Data (template, node type) upgrade: No 
	* Is there a performance risk/cost?: No
