====== Deployment procedure to an application server ======

1. Make sure you have correct: 
1.1. settings.xml. There should be the correct application server version (e.g <exo.projects.app.tomcat.version>) etc.
1.2. exo directory structure.
1.3. Maven version 2.2.1 (or higher).
1.4. Make sure you have run mvn insctal within ./config folder.  
2. Run "mvn -f product-exo-jcr-as-tomcat6.xml clean install antrun:run" command.
3. If the command has executed successfully, go to exo-tomcat and run "eXo run" command.
4. You may use other product-exo-jcr-as* and an application server if you need. 

Application server's specific configs is placed in product-patches/as/ folder.

exo-configuration.xml - the main config file for the samles.
For now you may automatically deploy our samples for tomcat (WARs, JARs should be in tomcat/lib) and jboss (EAR) application server.  
Deployment procedure has been tested with tomcat-6.0.24, jboss-5.1.0.GA.