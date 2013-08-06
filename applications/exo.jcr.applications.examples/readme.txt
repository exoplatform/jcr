This project is an example of how scopes managed out of the box by eXo Kernel can be used. 

It is composed of a simple rest component that calls twice the getId method of each "Id providers".
They all have a dedicated scope to allow you to see the differences that we can have from one scope to another.

To test it, you will need to deploy the artifact with the latest version of javassist.

You will need to access to http://localhost:8080/browser in order to create a session, then you will be
able to call the rest component thanks to the URL http://localhost:8080/browser/rest/scopes. To better understand
the lifecycle of the "Id provider" with a session scope, you can login then logout from http://localhost:8080/browser.

NB: This project has been designed for eXo JCR in standalone mode only