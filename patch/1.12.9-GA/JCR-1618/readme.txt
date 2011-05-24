Summary

    Status: javax.jcr.InvalidItemStateException: Node can't be saved No same-name sibling exists with index 2
    CCP Issue: CCP-928, Product Jira Issue: JCR-1618.
    Complexity: Low

The Proposal
Problem description

What is the problem to fix?
This issue is only reproduced with Mysql engine type InnoDB(not reproduced with MySQL MyIsam)

when we try to create a new nodetype with same name sibling we get this exception :
12:54:42,125 ERROR [STDERR] javax.jcr.InvalidItemStateException: Node can't be saved []:1[]test2000:3. No same-name sibling exists with index 2.
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager.checkPersistedSNS(WorkspacePersistentDataManager.java:586)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager.checkSameNameSibling(WorkspacePersistentDataManager.java:558)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager.doAdd(WorkspacePersistentDataManager.java:655)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager$ChangesLogPersister.save(WorkspacePersistentDataManager.java:387)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager.save(WorkspacePersistentDataManager.java:165)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager.access$101(CacheableWorkspaceDataManager.java:54)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager$SaveInTransaction.action(CacheableWorkspaceDataManager.java:262)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.TxIsolatedOperation.txAction(TxIsolatedOperation.java:92)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager$SaveInTransaction.txAction(CacheableWorkspaceDataManager.java:268)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.TxIsolatedOperation.perform(TxIsolatedOperation.java:210)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager.save(CacheableWorkspaceDataManager.java:526)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.ACLInheritanceSupportedWorkspaceDataManager.save(ACLInheritanceSupportedWorkspaceDataManager.java:225)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.persistent.VersionableWorkspaceDataManager.save(VersionableWorkspaceDataManager.java:244)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager.save(TransactionableDataManager.java:366)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.core.SessionDataManager.commit(SessionDataManager.java:1389)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.core.ItemImpl.save(ItemImpl.java:684)
12:54:42,125 ERROR [STDERR]     at org.exoplatform.services.jcr.impl.core.SessionImpl.save(SessionImpl.java:935)
12:54:42,125 ERROR [STDERR]     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
12:54:42,125 ERROR [STDERR]     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
12:54:42,125 ERROR [STDERR]     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
12:54:42,125 ERROR [STDERR]     at java.lang.reflect.Method.invoke(Method.java:597)

How to reproduce :
you can find in attachment a sample usecase you need just to deploy the bsh script under your jboss home and you will get this exception

we create in this case a node "test" then we save the session and if we try to create a new node with the same name we get InvalidItemState Exception :
print("create first Node");
            sess = getSessionFromExoAPI();
            print(sess);
            sess.getRootNode().addNode("test");
            sess.save();
            print("create second Node");
            print("Initializing");
            sess = getSessionFromExoAPI();
            print(sess);
            print("create second Node with the same name");
            sess.getRootNode().addNode("test");
            sess.save();
Fix description

How is the problem fixed?

Check-sns-new-connection is set to false by default

Patch information:
JCR-1618.patch

Tests to perform

Reproduction test

    No

Tests performed at DevLevel

    functional tests in jcr-core project. Manual testing wih JBoss bundle.

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    No

Configuration changes

Configuration changes:
    No

Will previous configuration continue to work?
    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
    No

Is there a performance risk/cost?
    No

Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

