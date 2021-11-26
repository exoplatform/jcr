/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.integration;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: JCRAPITest.java 12419 2008-03-26 16:47:49Z pnedonosko $
 */
public class JCRAPITest extends TestSuite
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.JCRAPITest");

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
