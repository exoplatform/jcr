Summary

    * Status: Problem of EventListener not triggered when renaming a document
    * CCP Issue: CCP-1106, Product Jira Issue: JCR-1671.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
    * We want to add a listener that implements javax.jcr.observation.EventListener to intercept the event of renaming.
      For this, there are five types of event: NODE_ADDED, NODE_REMOVED, PROPERTY_ADDED, PROPERTY_REMOVED, PROPERTY_CHANGED
      Normally, the event of renaming should be considered as "delete" and "add" so when renaming a node the two events NODE_ADDED and NODE_REMOVED must be triggered which is not our case.

Fix description
Problem analysis
    * The root cause is incorrect usage of JCR Sessions. UI performs operations on non-alive session. Such sessions are removed from SessionRegistry and therefore not found when broadcasting events. But session instance is mandatory there. 

How is the problem fixed?
    * Inject a Session instance to PlainChangesLogImpl class, that is invoked in dataflow. Mentioned Session instance is retrieved from log when preparing the events to broadcast.

Patch file: JCR-1671.patch

Tests to perform

Reproduction test

    * Platform behavior successfully reproduced within JCR-level unit test org.exoplatform.services.jcr.api.observation.TestSessionsObservation#testMoveOnClosedSession()

Tests performed at DevLevel

    * Both steps to reproduce given in issue's description and reproduction unit test.

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

    * Internal components changed: Interface PlainChangesLog extended with new method and PlainChangesLogImpl and its derivative classes updated. New field and group of new constructors introduced. This doesn't affect serialization.

Is there a performance risk/cost?

    * Session instance can be swept by GC a bit later, comparing to previous state because it is handled by log instance. There is no performance risk.

Validation (PM/Support/QA)

PM Comment
* Patch validated

Support Comment
* Patch validated

QA Feedbacks
*

