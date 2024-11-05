/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.script.groovy;

import b.ImportedClass;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.StandaloneContainer;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class GroovyInstantiatorTest extends TestCase
{

   private GroovyScriptInstantiator groovyScriptInstantiator;

   private GroovyScriptInstantiator jarjarGroovyScriptInstantiator;

   /*
    * (non-Javadoc)
    * @see junit.framework.TestCase#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      StandaloneContainer.setConfigurationPath("src/test/resources/conf/standalone/test-configuration.xml");
      ExoContainer container = StandaloneContainer.getInstance();
      groovyScriptInstantiator =
         (GroovyScriptInstantiator)container.getComponentInstance(GroovyScriptInstantiator.class);
      jarjarGroovyScriptInstantiator =
         (GroovyScriptInstantiator)container.getComponentInstance("JarJarGroovyScriptInstantiator");
      assertNotNull(groovyScriptInstantiator);
      assertNotNull(jarjarGroovyScriptInstantiator);
   }

   public void testGroovyScriptInstantiatorSimple() throws Exception
   {
      String url = Thread.currentThread().getContextClassLoader().getResource("Book.groovy").toString();
      GroovyObject groovyObject = (GroovyObject)groovyScriptInstantiator.instantiateScript(url);
      /*
       * --- Groovy code --- def title = "Groovy in Action" def author =
       * "Andrew Glover" def price = 20.10 def isdn = "1234567890987654321"
       */
      assertEquals("Andrew Glover", groovyObject.getProperty("author"));
      assertEquals("Groovy in Action", groovyObject.getProperty("title"));
      assertEquals("1234567890987654321", groovyObject.getProperty("isdn"));
      assertEquals(20, groovyObject.getProperty("price"));
      groovyObject.setProperty("price", 10);
      assertEquals(10, groovyObject.getProperty("price"));
   }

   public void testGroovyScriptInstantiatorInjection() throws Exception
   {
      String url = Thread.currentThread().getContextClassLoader().getResource("TestInjection.groovy").toString();
      GroovyObject groovyObject = (GroovyObject)groovyScriptInstantiator.instantiateScript(url);
      assertNotNull(groovyObject.getProperty("sampleComponent"));
      assertEquals("sample component", ((SampleComponent)groovyObject.getProperty("sampleComponent")).getAbout());
   }

   public void testGroovyScriptInstantiatorXML() throws Exception
   {
      String url =
         Thread.currentThread().getContextClassLoader().getResource("TestSimpleXMLGenerator.groovy").toString();
      GroovyObject groovyObject = (GroovyObject)groovyScriptInstantiator.instantiateScript(url);
      groovyObject.invokeMethod("generateXML", new Object[]{new Book()});
   }

   public void testGroovyScriptJarJar() throws Exception
   {
      String url = Thread.currentThread().getContextClassLoader().getResource("TestJarJar.groovy").toString();
      GroovyObject groovyObject = (GroovyObject)jarjarGroovyScriptInstantiator.instantiateScript(url);
      Object field = groovyObject.getProperty("field");
      assertNotNull(field);
      assertTrue("Was expecting object " + field + " to be an instance of class " + ImportedClass.class.getName()
         + "instead of class " + field.getClass().getName(), field instanceof ImportedClass);
   }
}
