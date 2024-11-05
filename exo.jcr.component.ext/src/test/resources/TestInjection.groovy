package org.exoplatform.script.groovy.test

public class TestInjection {
  
  def org.exoplatform.services.script.groovy.SampleComponent sampleComponent
  
  // Object in constructor must be in container
  public TestInjection(org.exoplatform.services.script.groovy.SampleComponent sampleComponent) {
    this.sampleComponent = sampleComponent;
  }
  
}