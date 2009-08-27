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
package org.exoplatform.services.jcr.integration;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: JCRAPITest.java 12419 2008-03-26 16:47:49Z pnedonosko $
 */
public class JCRAPITest
   extends TestSuite
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.JCRAPInew");

   public static Test suite() throws InterruptedException
   {
      return new JCRAPITest();
   }

   public JCRAPITest()
   {

      TestSuite suite = new TestSuite("javax.jcr init tests");
      suite.addTestSuite(PrepareTestRepository.class);
      addTest(suite);

      addTest(org.apache.jackrabbit.test.api.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.query.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
      addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
   }
}
