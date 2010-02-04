MAVEN_OPTS="-Duser.language=en -Duser.region=us -Dorg.exoplatform.jcr.monitor.jdbcMonitor $MAVEN_OPTS "
mvn  clean test -Prun-tck
