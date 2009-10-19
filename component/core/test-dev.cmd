@set MAVEN_OPTS=-Duser.language=en -Duser.region=us %MAVEN_OPTS% -Dmaven.test.skip=true -Dexo.devtest.skip=false -DforkMode=never 

@start mvn clean test