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
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.WorkspaceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

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

   public void testCannotGetImportContentHandlerWhenNodeIsProtected() throws ItemExistsException,
      PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
      RepositoryException
   {
      workspace.getSession().getRootNode().addNode("someNode", "exo:myTypeJCR1703");

      try
      {
         workspace.getImportContentHandler("/someNode/exo:myChildNode", 0);
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testCannotGetImportContentHandlerWhenNodeIsLocked() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, RepositoryException
   {
      Node someNode = workspace.getSession().getRootNode().addNode("someNode");
      someNode.addMixin("mix:lockable");
      session.save();

      someNode.lock(true, false);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());

      try
      {
         session2.getWorkspace().getImportContentHandler("/someNode", 0);
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         someNode.unlock();
      }
   }

   public void testImportXML() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException
   {
      Node folder = root.addNode("folder");
      Node file = folder.addNode("file");
      session.save();

      File destFile = File.createTempFile("testExportImportValuesSysView", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);
      session.exportSystemView(file.getPath(), outStream, false, false);
      outStream.close();

      folder.remove();
      session.save();

      ((WorkspaceImpl)workspace).importXML(root.getPath(), new FileInputStream(destFile),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, false);

      session.save();

      assertNotNull(root.getNode("file"));
   }

   public void testImportXMLInProtectedNode() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException
   {
      Node folder = workspace.getSession().getRootNode().addNode("folder");
      Node file = folder.addNode("file");
      Node protectedNode = folder.addNode("someNode", "exo:myTypeJCR1703").getNode("exo:myChildNode");
      session.save();

      File destFile = File.createTempFile("testExportImportValuesSysView", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);
      session.exportSystemView(file.getPath(), outStream, false, false);
      outStream.close();
      file.remove();
      session.save();

      try
      {
         ((WorkspaceImpl)workspace).importXML(protectedNode.getPath(), new FileInputStream(destFile),
            ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, false);
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testImportXMLInLockedNode() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException
   {
      Node folder = workspace.getSession().getRootNode().addNode("folder");
      Node file = folder.addNode("file");
      Node lockedNode = folder.addNode("someNode");
      lockedNode.addMixin("mix:lockable");
      session.save();
      
      lockedNode.lock(true, false);

      File destFile = File.createTempFile("testExportImportValuesSysView", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);
      session.exportSystemView(file.getPath(), outStream, false, false);
      outStream.close();
      file.remove();
      session.save();

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());
      
      try
      {
         ((WorkspaceImpl)session2.getWorkspace()).importXML(lockedNode.getPath(), new FileInputStream(destFile),
            ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, false);
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         lockedNode.unlock();
      }
   }

   public void testMoveNodeWhenParentNodeIsLocked() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node folder = workspace.getSession().getRootNode().addNode("folder");
      Node file = folder.addNode("file");
      Node lockedNode = folder.addNode("someNode");
      lockedNode.addMixin("mix:lockable");
      session.save();

      lockedNode.lock(true, false);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());

      try
      {
         ((WorkspaceImpl)session2.getWorkspace()).move(lockedNode.getPath(), file.getPath());
      }
      catch (LockException e)
      {
      }
   }

   public void testCloneNodeWhenClonedInTheSameNode() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node testNode = workspace.getSession().getRootNode().addNode("testNode");
      session.save();

      try
      {
         ((WorkspaceImpl)workspace).clone(workspace.getName(), testNode.getPath(), testNode.getPath(), false);
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testResotoreVersionWhenArrayVersionsHaventSomeVersion() throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node nodeA = workspace.getSession().getRootNode().addNode("versionableNodeA");
      nodeA.addMixin("mix:versionable");
      root.save();
      nodeA.checkin();// v.1
      nodeA.checkout();

      Node nodeB = nodeA.addNode("Subnode B");
      nodeA.save();
      nodeB.addMixin("mix:versionable");
      nodeA.save();
      nodeB.checkin();

      Node nodeC = nodeA.addNode("Subnode C");
      nodeA.save();
      nodeC.addMixin("mix:versionable");
      nodeA.save();
      nodeC.checkin();
      nodeC.checkout();
      nodeC.setProperty("Property Y", nodeB);
      nodeC.save();
      Version vC = nodeC.checkin();
      nodeC.checkout();

      nodeB.checkout();
      nodeB.setProperty("Property X", nodeC);
      nodeB.save();
      Version vB = nodeB.checkin();
      nodeB.checkout();

      nodeA.setProperty("Property", "property of subnode");
      nodeA.save();
      Version vA = nodeA.checkin();
      nodeA.checkout();

      nodeB.remove();
      nodeC.remove();
      nodeA.save();

      session.save();

      Version[] vs = new Version[]{vB, vC};

      try
      {
         ((WorkspaceImpl)session.getWorkspace()).restore(vs, true);
         fail();
      }
      catch (VersionException e)
      {
      }
   }
}
