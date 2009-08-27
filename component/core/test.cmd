@set MAVEN_OPTS=-Duser.language=en -Duser.region=us %MAVEN_OPTS% -Dmaven.test.skip=false -Dexo.tck.skip=true -Dexo.devtest.skip=true -DforkMode=never 

@start mvn clean test