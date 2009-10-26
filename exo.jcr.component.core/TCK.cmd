@set MAVEN_OPTS=-Duser.language=en -Duser.region=us -Dmaven.test.skip=false -DforkMode=once %MAVEN_OPTS% 

@start mvn clean install -Prun-tck
