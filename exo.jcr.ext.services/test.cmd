@set MAVEN_OPTS=-Duser.language=en -Duser.region=us %MAVEN_OPTS% -Dexo.test.skip=false -Dexo.tck.skip=true -Dexo.devtest.skip=true -Dexo.test.forkMode=once 

@start mvn clean test