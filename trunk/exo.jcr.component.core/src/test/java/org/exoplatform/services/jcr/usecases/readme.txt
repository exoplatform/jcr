How to make new JCR UseCase test:
--------------------------------

0. Make sure you understand how JUnit works :) (http://www.junit.org)
1. Make Test unit extended org.exoplatform.services.jcr.usecases.BaseUsecasesTest
2. Write your tests, to simplify you can use pre-initialized 
   class variables: repository, session, root etc.
3. Recommendations:
  - Use unique sub-root node name for each case. Use test method name as sub-root
    node name for example.
  - Make sure you remove permanently (i.e. using save()) the test item created for 
    testing either in test method itself or in tearDown()
  - It is highly recommended to make some comments :)

Here is and example of use case test:

public class SampleUseCaseTest extends BaseUsecasesTest {
 
  /**
   * Sample test. An example how to make it 
   * @throws Exception
   */
  public void testSomething() throws Exception {
    // make sub-root with unique name;
    Node subRootNode = root.addNode("testSomething");
    
    // make the structure under subRootNode if you need so...
    subRootNode.setProperty("someProperty", "someValue");
    
    // and save if you need so...
    session.save();
    
    
    // and test 
    this.assertNotNull(subRootNode);
    
    // you have to remove and save it at the end of method(recommended) or at the 
    // tearDown() as well
    subRootNode.remove();
    session.save();
    
  }
}

How to run JCR UseCase test:
--------------------------------
1. Make sure you have uncommented:
    <testSourceDirectory>src/test</testSourceDirectory>
  and commented
    <!-- testSourceDirectory>src/TCK</testSourceDirectory -->
  in the /exo-jcr/services/jcr/impl/pom.xml
2. Include your test into maven-surefire-plugin  
  like
      ....
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <includes>
            <include>**/usecases/SomeTest.java</include>
      ....   
  NOTE: DO NOT COMMIT pom.xml with YOUR CHANGES PLEASE!
3. Make sure tests are enabled in 
  your_home_dir/.m2/settings.xml:
   <maven.test.skip>false</maven.test.skip>
  to enable txt output also set
   <surefire.useFile>true</surefire.useFile>
4. Call mvn [clean] install in /exo-jcr/services/jcr/impl
5. You can find the test results at the target/surefire-reports folder
