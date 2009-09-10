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
package org.exoplatform.services.jcr.api;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll extends TestCase
{

   public static Test suite()
   {
      TestSuite suite = new TestSuite("exo.jcr tests");

      suite.addTestSuite(org.exoplatform.services.jcr.api.accessing.TestAccessRepository.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestItem.class);
      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestItemVisitor.class);
      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestNode.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestProperty.class);
      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestSession.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestValue.class);
      suite.addTestSuite(org.exoplatform.services.jcr.api.reading.TestWorkspace.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestSession.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestItem.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestAddNode.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestAssignMixin.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestCorrespondingNode.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestRemove.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestNodeReference.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestSetProperty.class);
      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestValue.class);

      suite.addTestSuite(org.exoplatform.services.jcr.api.writing.TestCopyNode.class);

      return suite;
   }
}
