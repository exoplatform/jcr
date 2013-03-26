Thank you for your interest in eXo JCR

EAR deploy
==========

eXo JCR was tested under JBoss-5.1.0.GA application server

JBoss-7.1.1.Final

  1. Configuration

    * Copy exo.jcr.ispn.ear.ear into $jboss_home/standalone/deployments
    * Copy standalone.conf and standalone.conf.bat into $jboss_home/bin
    * Create $jboss_home/standalone/configuration/exo-conf folder if it doesn't exist.
    * Put exo-configuration.xml into jboss_home/standalone/configuration/exo-conf/exo-configuration.xml
    * Replace standalone.xml into jboss_home/standalone/configuration/standalone.xml

  2. Start Up

To launch it, go to the bin directory and execute the following command:

In Unix environment

* "./standalone.sh"

In Windows environment

* "standalone.bat"
