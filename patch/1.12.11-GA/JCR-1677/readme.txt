Summary

    * Status: Could not intercept any jcr action listener only in the trunk of a predefined workspace
    * CCP Issue: CCP-1120, Product Jira Issue: JCR-1677.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
* When removing a node using the File Explorer contextual menu, the component org.exoplatform.ecm.webui.component.explorer.rightclick.manager.DeleteManageComponent would be executed to remove a node or to move it to Trash if it's in the same workspace.  Removing nodes from "collaboration" workspace using UI does not execute a JCR node remove event, but it just moves the node to Trash folder, that means it changes the Path of the node to collaboration:/Trash folder.
* However, if i configure my listener to be triggered on the workspace collaboration, and i delete a document from another workspace, the listner will be triggered.

Fix description
Problem analysis
    * Workspace name wasn't handled when generating condition list. That's why this condition was skipped when broadcasting events, so Listeners registered on one workspace, received events from another one. 

How is the problem fixed?
    * Pass the workspace name to SessionActionInterceptor via constructor argument and set it into condition's map.

Patch file: JCR-1677.patch

Tests to perform

Reproduction test
    * Manual, given in issue description.

Tests performed at DevLevel

    * Manual testing, mentioned in the description of the issue.

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

    * The signature of SessionActionInterceptor's constructor changed: new argument added. Doesn't influence on compatibly with other projects because SessionActionInterceptor is an internal component and not the part of public API. But fix in general may influence some projects because condition for workspace name is now properly handled and events with non-matching names won't be broadcasted.

Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
* Patch validated

Support Comment
* Patch validated

QA Feedbacks
*

