Summary

    * Status: Allow to create sessions from ACLs
    * CCP Issue: CCP-1032, Product Jira Issue: JCR-1636.
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
In authenticated mode, there's a request to cache contents and share the cache for all users. We need to retrieve the contents first then we use a portlet cache to cache the resulting markup and share it between users.

We have three ways to retrieve contents from the JCR :

    * System: if we use this, we will end up with contents visible by unauthorized users
    * User session: if we use this, the first to access the contents will cache the results. Thus, the resulting markup is based on the first user to access them. The resulting effect is like the System session, we end up with possible visible contents, not authorized for some users.
    * Anonymous: The last one, if we're authenticated, we can still create an anonymous session. But, we will then see the "public" contents only (with "Any READ" permission). For an intranet need, it's not enough. Most of the time, a folder will contain public contents ("Any READ") and intranet contents ("*:/platform/users READ"). Thus, when authenticated, you see more contents than in public mode.

So, the request is to be able for WCM Services to create a fake session (like we do with the anonymous one)
Here is a proposal of a possible call:
SessionProvider.createProvider(List<AccessControlEntry> accessList);
accessList==null or empty: anonymous session
ACL = {"*:/platform/users READ"} => we have a private session

Fix description

How is the problem fixed?

    * Create session with custom set of ACL

Patch file: JCR-1636.patch

Tests to perform

Reproduction test
* N/A

Tests performed at DevLevel

    * Functional tests

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

    * No

Validation (PM/Support/QA)

PM Comment
* Patch approved

Support Comment
*

QA Feedbacks
*

