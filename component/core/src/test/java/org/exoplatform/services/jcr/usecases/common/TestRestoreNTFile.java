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
package org.exoplatform.services.jcr.usecases.common;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.pham@exoplatform.com
 * phamvuxuanhoa@yahoo.com Jul 3, 2006
 */
public class TestRestoreNTFile
   extends BaseUsecasesTest
{

   public void testRestoreNTFile() throws Exception
   {
      Session session = repository.getSystemSession(repository.getSystemWorkspaceName());
      Node ntFile = session.getRootNode().addNode("test", "nt:file");
      ntFile.addNode("jcr:content", "nt:folder");
      session.save();

      Node testNTFile = session.getRootNode().getNode("test");
      assertTrue("nt:file".equals(testNTFile.getPrimaryNodeType().getName()));
      assertTrue(testNTFile.hasProperty("jcr:created"));
      assertNotNull(testNTFile.getProperty("jcr:created").getValue());
      ntFile.addMixin("mix:versionable");
      session.save();
      Version ver1 = ntFile.checkin();
      ntFile.checkout();
      Version ver2 = ntFile.checkin();
      ntFile.checkout();
      Version ver3 = ntFile.checkin();
      ntFile.checkout();
      session.save();
      Version baseVersion = ntFile.getBaseVersion();
      assertEquals(ver3, baseVersion);
      try
      {
         ntFile.restore(ver2, false);
         baseVersion = ntFile.getBaseVersion();
         assertEquals(ver2, baseVersion);
      }
      catch (Exception e)
      {
         log.error("exception when restore version of nt:file", e);
         fail("========> exception when restore version of nt:file:\n\n" + e.getMessage());
      }
      testNTFile.remove();
      session.save();
   }
}
