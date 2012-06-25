Summary
	- Issue title: RepositoryCheckController logs ClassNotFoundException in its report
	- CCP Issue:  N/A
	- Product Jira Issue: JCR-1792.
	- Complexity: Low

Proposal

 
Problem description

What is the problem to fix?
	- RepositoryCheckController logs ClassNotFoundException in its report

Fix description

Problem analysis
	- Some factory class is  absent in classpath if JConsole application, therefore was not possible to create initial context

How is the problem fixed?
	- Pass datasource to LockConsistencyChecker and avoid looking up from initial context

Tests to perform

Reproduction test
	- Start PLF tomcat with JMX enable
	- Add JMX config to start_eXo.sh
            JMX_CONFIG="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=8004 -Dcom.sun.management.jmxremote.authenticate=false"
            CATALINA_OPTS="$JVM_OPTS $CATALINA_OPTS $EXO_PROFILES $JMX_CONFIG"
	- After PLF server starts successfully, launch JConsole
	- Switch to Tab "MBeans", go to exo > "portal" > "repository" > RepositoryCheckController > Operations
	- Start CheckDatase -> Exception due to NamingException

Tests performed at DevLevel
	* Functional testing, manual testing

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	*

Changes in Selenium scripts 
	*

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* NO

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No
	* Data (template, node type) migration/upgrade: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	*

QA Feedbacks
	*
