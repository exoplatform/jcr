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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestRepositoryManagement.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestRepositoryManagement extends JcrImplBaseTest
{

   public static int BINDED_DS_COUNT = 100;

   private static boolean isBinded = false;

   private final int lastDS = 0;

   private WorkspaceEntry wsEntry;

   private boolean isDefaultWsMultiDb;

   private final TesterConfigurationHelper helper;

   public TestRepositoryManagement()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstance();
   }


   public void testAddNewRepository() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);
      assertNotNull(repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName()).getRootNode());
   }

   public void testAddNewRepositoryWithSameName() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      try
      {
         RepositoryEntry rEntry = helper.createRepositoryEntry(false, null, null);
         rEntry.setName(repository.getConfiguration().getName());

         helper.createRepository(container, rEntry);
         fail();
      }
      catch (Exception e)
      {
         // ok
      }
   }

   public void testCanRemove() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      assertFalse(service.canRemoveRepository(repository.getConfiguration().getName()));
      session.logout();
      assertTrue(service.canRemoveRepository(repository.getConfiguration().getName()));
   }

   public void testInitNameSpaces() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      assertEquals("http://www.apache.org/jackrabbit/test", session.getNamespaceURI("test"));
      assertEquals("http://www.exoplatform.org/jcr/test/1.0", session.getNamespaceURI("exojcrtest"));
   }

   public void testInitNodeTypes() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      // check if nt:folder nodetype exists
      session.getRootNode().addNode("folder", "nt:folder");
      session.save();
   }

   public void testRemove() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      service.removeRepository(repository.getConfiguration().getName());

      try
      {
         service.getRepository(repository.getConfiguration().getName());
      }
      catch (Exception e)
      {

      }
   }
}
