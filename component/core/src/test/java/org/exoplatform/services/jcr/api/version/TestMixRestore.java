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
package org.exoplatform.services.jcr.api.version;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS 31.08.2006 [VO]
 * 
 * @author <a href="mailto:vitaliy.obmanjuk@exoplatform.com.ua">Vitaliy Obmanjuk</a>
 * @version $Id: TestMixRestore.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestMixRestore
   extends BaseVersionTest
{

   private Node testVersionable = null;

   public void setUp() throws Exception
   {
      super.setUp();
      testVersionable = root.addNode("testVersionable", "nt:unstructured");
      testVersionable.addMixin("mix:versionable");
      root.save();
   }

   protected void tearDown() throws Exception
   {
      testVersionable.checkout();
      testVersionable.remove();
      root.save();
      super.tearDown();
   }

   public void testVersionHistoryTree() throws Exception
   {
      Node testref = testVersionable.addNode("testref");
      testref.addMixin("mix:referenceable");
      testVersionable.save();
      Version ver1 = testVersionable.checkin(); // v1
      testVersionable.restore(ver1.getName(), true);
      testVersionable.restore(ver1, true);
   }
}
