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
package org.exoplatform.services.jcr.usecases.version;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS
 * 
 * @version $Id: RemoveVersionAfterRestoreTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class RemoveVersionAfterRestoreTest extends BaseUsecasesTest
{

   public void testRemoveVersionAfterRestore() throws Exception
   {
      Node node1 = root.addNode("Node1", "nt:unstructured");
      node1.addMixin("mix:versionable");
      root.save();
      Version ver1 = node1.checkin();
      node1.checkout();
      Version ver2 = node1.checkin();
      node1.checkout();
      Version ver3 = node1.checkin();
      node1.checkout();
      node1.restore(ver2, true);
      node1.getVersionHistory().removeVersion(ver1.getName());
      node1.getVersionHistory().removeVersion(ver3.getName());
      // session.save(); // unnecessary here
      assertNotNull(session.getRootNode().getNode("Node1"));
      node1.restore(ver2, true);
      assertNotNull(session.getRootNode().getNode("Node1"));
   }
}
