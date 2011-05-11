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
package org.exoplatform.services.jcr.api.reading;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestWorkspace.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestWorkspace extends JcrAPIBaseTest
{

   public void testGetSession()
   {
      assertEquals(session, workspace.getSession());
   }

   public void testGetName()
   {
      assertEquals("ws", workspace.getName());
   }

   public void testGetQueryManager() throws Exception
   {
      assertNotNull(workspace.getQueryManager());
   }

   public void testGetNamespaceRegistry() throws Exception
   {
      assertNotNull(workspace.getNamespaceRegistry());
   }

   public void testGetNodeTypeManager() throws Exception
   {
      assertNotNull(workspace.getNodeTypeManager());
   }

   public void testGetAccessibleWorkspaceNames() throws Exception
   {
      log.debug(workspace.getAccessibleWorkspaceNames()[0]);
      assertNotNull(workspace.getAccessibleWorkspaceNames());
   }

}
