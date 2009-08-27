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
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 15.08.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestWorspaceReadOnly.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestWorspaceReadOnly
   extends JcrImplBaseTest
{

   private WorkspaceContainerFacade wsFacade;

   private PersistentDataManager dataManager;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      wsFacade = repository.getWorkspaceContainer(session.getWorkspace().getName());
      dataManager = (PersistentDataManager) wsFacade.getComponent(PersistentDataManager.class);
   }

   @Override
   protected void tearDown() throws Exception
   {
      dataManager.setReadOnly(false);
      super.tearDown();
   }

   public void testWorkspaceDataContainerStatus()
   {

      dataManager.setReadOnly(true);

      assertTrue(dataManager.isReadOnly());
   }

   public void testWorkspaceDataManager() throws Exception
   {

      dataManager.setReadOnly(true);

      WorkspacePersistentDataManager dm =
               (WorkspacePersistentDataManager) wsFacade.getComponent(WorkspacePersistentDataManager.class);

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
