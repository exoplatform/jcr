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
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.MembershipEntry;

import java.util.ArrayList;

import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestInitRepository.java 13891 2008-05-05 16:02:30Z pnedonosko $
 */
public class TestInitRepository extends JcrImplBaseTest
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.JCRTest");

   public void _testRepositoryServiceRegistration() throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      assertNotNull(service);
      RepositoryImpl defRep = (RepositoryImpl)service.getRepository();
      assertNotNull(defRep);
      String sysWs = defRep.getSystemWorkspaceName();
      assertFalse("Sys ws should not be    initialized for this test!!", defRep.isWorkspaceInitialized(sysWs)); // Default Namespaces
      // and NodeTypes
      NamespaceRegistry nsReg = defRep.getNamespaceRegistry();
      assertNotNull(nsReg);
      assertTrue(nsReg.getPrefixes().length > 0);
      NodeTypeManager ntReg = defRep.getNodeTypeManager();
      assertNotNull(ntReg);
      assertTrue(ntReg.getAllNodeTypes().getSize() > 0);
   }

   public void _testInitSystemWorkspace() throws Exception
   {

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep = (RepositoryImpl)service.getRepository();
      String sysWs = defRep.getSystemWorkspaceName();
      assertFalse("Sys ws should not be initialized for this test!!", defRep.isWorkspaceInitialized(sysWs));

      // TODO
      // defRep.initWorkspace(sysWs, "nt:unstructured");

      Session sess = defRep.getSystemSession(sysWs);

      Node root = sess.getRootNode();
      assertNotNull(root);

      assertNotNull(root.getNode("jcr:system"));

      assertNotNull(root.getNode("jcr:system/exo:namespaces"));
      sess.logout();

   }

   public void testInitRegularWorkspace() throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep = (RepositoryImpl)service.getDefaultRepository();
      String sysWs = defRep.getSystemWorkspaceName();

      String[] names = defRep.getWorkspaceNames();
      String wsName = null;
      for (int i = 0; i < names.length; i++)
      {
         if (!names[i].equals(sysWs))
         {
            wsName = names[i];
            break;
         }
      }
      if (wsName == null)
      {
         fail("not system workspace not found for test!!");
      }

      // TODO
      // defRep.initWorkspace(wsName, "nt:unstructured");

      // Session sysSess = defRep.getSystemSession(sysWs);
      Session sess = defRep.getSystemSession(wsName);
      // assertEquals(sysSess, sess);
      // log.info("sys>>"+sysWs+" "+sysSess);
      log.info("reg>>" + wsName + " " + sess);

      Node root = sess.getRootNode();
      assertNotNull(root);

      // root = sysSess.getRootNode();
      // assertNotNull(root);
      sess.logout();
   }

   public void testAutoInitRootPermition()
   {
      WorkspaceEntry wsEntry = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      AccessControlList expectedAcl = new AccessControlList();
      try
      {
         if (wsEntry.getAutoInitPermissions() != null)
         {
            expectedAcl.removePermissions(SystemIdentity.ANY);
            expectedAcl.addPermissions(wsEntry.getAutoInitPermissions());
         }
         AccessControlList acl = ((ExtendedNode)session.getRootNode()).getACL();
         assertTrue(expectedAcl.equals(acl));

      }
      catch (RepositoryException e)
      {
         fail(e.getLocalizedMessage());
      }
   }

   public void testCanRemoveWorkspaceWhenWorkspaceNotFound() throws RepositoryException,
      RepositoryConfigurationException
   {
      try
      {
         repository.canRemoveWorkspace(" ");
         fail();
      }
      catch (NoSuchWorkspaceException e)
      {         
      }
   }

   public void testCreateWorkspaceWhenWorkspaceHaventConfig()
   {
      try
      {
         repository.createWorkspace("someWorkspace");
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testGetSystemSessionWhenWorkspaceNotFound()
   {
      try
      {
         repository.getSystemSession(" ");
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testGetDynamicSessionWhenWorkspaceNotFound()
   {
      try
      {
         repository.getDynamicSession(" ", new ArrayList<MembershipEntry>());
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }
}