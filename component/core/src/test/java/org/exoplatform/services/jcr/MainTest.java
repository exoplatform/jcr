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
package org.exoplatform.services.jcr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * An example of testing servlets using httpunit and JUnit.
 **/
public class MainTest extends TestCase
{

   private Log log = ExoLogger.getLogger("jcr.Test");

   public static void main(String args[])
   {
      TestRunner.run(suite());
   }

   public static Test suite()
   {

      // System.setProperty("test.repository", "db1");
      TestSuite suite = new TestSuite("javax.jcr tests");

      // suite
      // .addTestSuite(org.exoplatform.services.jcr.api.accessing.TestAccessRepository.class);
      // suite
      // .addTestSuite(org.exoplatform.services.jcr.api.version.TestVersionHistory.class);

      suite.addTestSuite(MainTest.class);

      return suite;
   }

   public void testAddNode() throws Exception
   {

      log.error("test");
   }
}
