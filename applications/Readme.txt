====== Deployment procedure to an application server ======

I. Make sure you have correct: 
   * settings.xml. There should be the correct application server version (e.g <exo.projects.app.tomcat.version>) etc
   * exo directory structure
   * Maven version 3.0.3 (or higher)

II. Has been tested with such AS:
   * Tomcat 7.0.32
   * JBoss 5.1.0.GA
   * Jonas 4.10.4
   * Jetty 7.1.5.v20100705

III. Deployment
   If you want to deploy Tomcat, go to folder "exo.jcr.applications.tomcat" and run:
     * to deploy Tomcat 7.x.x run "mvn clean install -Pdeploy" command 
   If you want to deploy JBoss, Jonas or Jetty use exo.jcr.applications.jboss, exo.jcr.applications.jonas or exo.jcr.applications.jetty  respectively.

IV. If you want to deploy AS with eXo JCR based on ISPN use "ispn" profile in deployment command like this "mvn clean install -Pdeploy,ispn"

V. If the command has executed successfully, go to exo-working directory, choose deployed AS and run "bin/eXo-jbc.sh run" (or bin/eXo-ispn.sh run) command.

