package org.exoplatform.groovy.test

import javax.ws.rs.GET
import javax.ws.rs.Path

import dependencies.Dep1

@Path("groovy-test-dependency")
public class TestDependency {
  
  @GET
  def method() {
    return new Dep1().getName()
  }
  
}