package org.exoplatform.script.groovy.test

import a.ImportedClass

public class TestJarJar {

  def ImportedClass field = new ImportedClass();

  // Object in constructor must be in container
  public TestInjection() {
  }

}