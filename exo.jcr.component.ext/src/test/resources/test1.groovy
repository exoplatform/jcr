package org.exoplatform.groovy.test

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam

@Path("groovy-test")
public class Test1 {
  
  
  public Test1() {
  }
  
  @GET
  @Path("/groovy1/{param}/")
  public String method(@PathParam("param") String name) {
    def resp = "Hello from groovy to " + name
    return resp
  }
  
}