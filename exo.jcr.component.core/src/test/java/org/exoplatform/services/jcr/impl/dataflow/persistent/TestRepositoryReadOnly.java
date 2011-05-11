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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;

/**
 * Created by The eXo Platform SAS
 * Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * @version $Id: $
 */

public class TestRepositoryReadOnly extends JcrImplBaseTest
{

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      repository.getWorkspaceContainer(session.getWorkspace().getName());
      //dataContainer = (WorkspaceDataContainer) wsFacade.getComponent(WorkspaceDataContainer.class);
   }

   @Override
   protected void tearDown() throws Exception
   {
      repository.setState(repository.ONLINE);
      super.tearDown();
   }

   public void testRepositoryReadOnly() throws Exception
   {

      repository.setState(repository.READONLY);

      assertEquals(repository.READONLY, repository.getState());

      WorkspacePersistentDataManager dm =
         (WorkspacePersistentDataManager)(repository.getWorkspaceContainer(session.getWorkspace().getName()))
            .getComponent(WorkspacePersistentDataManager.class);

      try
      {
         dm.save(new PlainChangesLogImpl());
         fail("Read-only container should throw an ReadOnlyWorkspaceException");
      }
      catch (ReadOnlyWorkspaceException e)
      {
         // ok
      }
   }

}
