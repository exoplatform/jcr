@set MAVEN_OPTS=-Duser.language=en -Duser.region=us %MAVEN_OPTS% 

@start mvn clean install -Prun-tck
