MAVEN_OPTS="-Duser.language=en -Duser.region=us -Dmaven.test.skip=false -DforkMode=never -Dorg.exoplatform.jcr.monitor.jdbcMonitor $MAVEN_OPTS "
mvn  clean test -Prun-tck
