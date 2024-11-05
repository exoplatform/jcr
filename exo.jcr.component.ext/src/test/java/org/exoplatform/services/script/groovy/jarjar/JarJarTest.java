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
package org.exoplatform.services.script.groovy.jarjar;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class JarJarTest extends TestCase
{

   public static AssertionFailedError error = null;

   public JarJarTest()
   {
   }

   public JarJarTest(String s)
   {
      super(s);
   }

   private void testTop(TestScript script)
   {
      Mapping m1 = new Mapping();
      m1.addMapping("a", "b");
      Mapping m2 = new Mapping();
      m2.addMapping("a", "prefix1.a");

      // Transform a top package into a top package
      assertEquals("b", script.execute(m1));

      // Transform a top package into a prefixed package
      assertEquals("prefix1.a", script.execute(m2));
   }

   public void testTopClassLitteral() throws Exception
   {
      testTop(new TestScript("classlitteral1.groovy"));
      testTop(new TestScript("classlitteral_1.groovy"));
      testTop(new TestScript("import1.groovy"));
   }

   private void testPrefix(TestScript script) throws Exception
   {
      Mapping m1 = new Mapping();
      m1.addMapping("prefix1", "prefix2");
      Mapping m2 = new Mapping();
      m2.addMapping("prefix1.a", "a");
      Mapping m3 = new Mapping();
      m3.addMapping("prefix1.a", "prefix2.b");
      Mapping m4 = new Mapping();
      m4.addMapping("prefix1.a", "prefix1.b");

      // Transform the top package prefix
      assertEquals("prefix2.a", script.execute(m1));

      // Transform the full prefixed package
      assertEquals("a", script.execute(m2));

      // Transform the full prefixed package
      assertEquals("prefix2.b", script.execute(m3));

      // Transform the full prefixed package
      assertEquals("prefix1.b", script.execute(m4));
   }

   public void testPrefixClassLitteral() throws Exception
   {
      testPrefix(new TestScript("classlitteral2.groovy"));
      testPrefix(new TestScript("classlitteral_2.groovy"));
      testPrefix(new TestScript("import2.groovy"));
   }
}
