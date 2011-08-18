Summary

    * Status: Lists stored into the cache can be inconsistent in cluster environment
    * CCP Issue: N/A Product Jira Issue: JCR-1650.
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
In cluster environment for example if:
1. we have the list of all the children nodes of a given JCR node in the cluster node 1
2. this list has not been loaded in the cluster node 2
3. and we add a new child node in the cluster node 2

In this kind of usecase, since the list has not been loaded locally, we don't change the list content which has for consequences that the cluster node 1 doesn't have this new node in its list which is a consistency issue.
Fix description

How is the problem fixed?

    * Invalidate the list of child nodes on all cluster nodes when a new child has been added and the local cache has now the list of child nodes

Patch file: JCR-1650.patch

Tests to perform

Reproduction test

    * TestWorkspaceStorageCacheInClusterMode.java

Tests performed at DevLevel

    * functional testing jcr-core project

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
  * No

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?

    * After invalidation all cluster nodes are supposed to reload the list of child nodes if need

Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
*

QA Feedbacks
*
