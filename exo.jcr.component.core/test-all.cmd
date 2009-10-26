@set MAVEN_OPTS=-Duser.language=en -Duser.region=us %MAVEN_OPTS% -Dmaven.test.skip=false -DforkMode=once

@start mvn clean test -Prun-tck