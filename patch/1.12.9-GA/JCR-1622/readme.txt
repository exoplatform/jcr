Summary

    Status: Thread not stopped when the application is stopped
    CCP Issue: N/A, Product Jira Issue: JCR-1622.
    Complexity: Medium

The Proposal
Problem description

What is the problem to fix?
Threads are not ended when the application is stopped/restarted
so after restart we have multiple instances of these threads ( org.exoplatform.container.StandaloneContainer-db1_cmis1_cacheWorker, and the one for db1_system too and also HSQL Timer thread)

test case to reproduce:

    start tomcat with xcmis
    go to Tomcat Manager
    Stop xCMIS Application
    Start xCMIS Application
    With a jconsole you can see that you have several times the same threads.

Fix description

How is the problem fixed?

* Make components to be Startable and on stop method shutdown working threads.

Patch information:
JCR-1622.patch

Tests to perform

Reproduction test
* Steps to reproduce:

Tests performed at DevLevel
    cf above

Tests performed at QA/Support Level
    cf above

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
    N/A

Function or ClassName change
    core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/control/UIAddressBar.java

Is there a performance risk/cost?
    No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

