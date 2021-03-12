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
import org.exoplatform.services.jcr.impl.core.ItemImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestSession.java 12108 2008-03-19 14:26:36Z gazarenkov $
 */
public class TestSession extends JcrAPIBaseTest
{

   public void testGetRepository()
   {
      assertEquals(session.getRepository(), repository);
   }

   public void testCredentials() throws Exception
   {
      assertEquals(session.getUserID(), credentials.getUserID());

      assertEquals(0, session.getAttributeNames().length);
      assertNull(session.getAttribute("test"));

      CredentialsImpl c2 = new CredentialsImpl(this.credentials.getUserID(), credentials.getPassword());
      c2.setAttribute("test", "value");

      Session session2 = repository.login(c2);

      assertEquals(1, session2.getAttributeNames().length);
      assertEquals("value", session2.getAttribute("test"));

   }

   public void testGetWorkspace()
   {
      assertEquals(session.getWorkspace().getSession(), session);
   }

   public void testImpersonate() throws LoginException, RepositoryException
   {
      Session session2 = session.impersonate(new CredentialsImpl("user", new char[0]));
      assertNotSame(session, session2);
      /*
       * 6.2.1 The new Session is tied to a new Workspace instance. In other
       * words, Workspace instances are not re-used.
       */
      assertNotSame(session.getWorkspace(), session2.getWorkspace());
      /*
       * 6.2.1 However, the Workspace instance returned represents the same actual
       * persistent workspace entity in the repository as is represented by the
       * Workspace object tied to this Session.
       */
      assertEquals(session.getWorkspace().getName(), session2.getWorkspace().getName());
   }

   public void testLogout() throws Exception
   {
      Session localSession = repository.login(credentials, "ws");
      localSession.logout();
   }

   public void testGetRootNode() throws RepositoryException
   {
      assertNotNull(session.getRootNode());
      assertEquals("/", session.getRootNode().getPath());
   }

   public void testItem() throws RepositoryException
   {
      Node root = session.getRootNode();

      // Node
      Node node = root.addNode("testItem", "nt:folder").addNode("childNode2", "nt:file");
      assertNotNull(session.getItem("/testItem/childNode2"));

      // Property
      Property prop = root.setProperty("prop", "val");
      assertNotNull(session.getItem("/prop"));

      try
      {
         session.getItem("/not/found");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      assertTrue(session.itemExists("/testItem/childNode2"));
      assertFalse(session.itemExists("/not/found"));

   }

   public void testGetNodeByUUID() throws RepositoryException
   {

      Node root = session.getRootNode();
      Node folder = root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");

      Node contentNode = folder.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      try
      {
         //log.debug("SDM before save: "+session.getTransientNodesManager().dump())
         // ;
         session.save();
         // child = child.getNode("jcr:content");
         assertNotNull(contentNode.getUUID());
         // System.out.println("LOC>>>"+session.getNodesManager().getLocation(
         // contentNode.getUUID()));
         Node n = session.getNodeByUUID(contentNode.getUUID());
         assertNotNull(n);
         assertEquals(contentNode.getPath(), n.getPath());
      }
      finally
      {
         // folder.refresh(false);
         if (log.isDebugEnabled())
         {
            log.debug("SDM before remove: " + session.getTransientNodesManager().dump());
         }
         folder.remove();
         session.save();
      }

   }
   
   public void testGetPropertyByIdentifier() throws RepositoryException
   {

      Node root = session.getRootNode();
      Node folder = root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");

      Node contentNode = folder.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      

      try
      {
         //log.debug("SDM before save: "+session.getTransientNodesManager().dump())
         // ;
         session.save();
         Property contentNodeJcrdataProperty = contentNode.getProperty("jcr:data");
         String contentNodeJcrdataPropertyIdentifier = ((ItemImpl) contentNodeJcrdataProperty).getInternalIdentifier();
         assertNotNull(contentNodeJcrdataProperty);
         Property jcrdataProperty = session.getPropertyByIdentifier(contentNodeJcrdataPropertyIdentifier);
         assertNotNull(jcrdataProperty);
         assertEquals(jcrdataProperty.getPath(), contentNodeJcrdataProperty.getPath());
      }
      finally
      {
         // folder.refresh(false);
         if (log.isDebugEnabled())
         {
            log.debug("SDM before remove: " + session.getTransientNodesManager().dump());
         }
         folder.remove();
         session.save();
      }

   }

   public void testGetAllNamespacePrefixes() throws RepositoryException
   {
      assertTrue(Arrays.asList(session.getWorkspace().getNamespaceRegistry().getPrefixes()).containsAll(
         Arrays.asList(session.getAllNamespacePrefixes())));
   }

   public void testGetImportContentHandlerCheckLockException() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node testNode = root.addNode("test");
      testNode.addMixin("mix:lockable");
      session.save();

      testNode.lock(true, true);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());
      
      try
      {
         session2.getImportContentHandler("/test", 0);
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         testNode.unlock();
      }
   }

   public void testGetImportContentHandlerCheckConstraintViolationException() throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      root.addNode("someNode", "exo:myTypeJCR1703");

      try
      {
         session.getImportContentHandler("/someNode/exo:myChildNode", 0);
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testImportXMLCheckLockException() throws ItemExistsException, PathNotFoundException,
      NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException,
      IOException
   {
      Node testNode = root.addNode("testNode");
      testNode.setProperty("exo:title", "testNode");
      testNode.addMixin("mix:versionable");

      Node testNodeImport = root.addNode("testNodeImport");
      testNodeImport.addMixin("mix:lockable");
      session.save();
      
      testNodeImport.lock(true, true);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      testNode.getSession().exportDocumentView(testNode.getPath(), bos, false, false);
      ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());

      try
      {
         session2.importXML(testNodeImport.getPath(), is, 1);
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         testNodeImport.unlock();
      }
   }

   public void testImportXMLCheckConstraintViolationException() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException, IOException
   {
      Node testNode = root.addNode("testNode");
      testNode.setProperty("exo:title", "testNode");
      testNode.addMixin("mix:versionable");

      root.addNode("someNode", "exo:myTypeJCR1703");
      session.save();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      testNode.getSession().exportDocumentView(testNode.getPath(), bos, false, false);
      ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());

      try
      {
         session.importXML("/someNode/exo:myChildNode", is, 1);
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testGetNodeByIdentifierWhenNodeNotFound() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      try
      {
         session.getNodeByIdentifier("someidentifier");
         fail();
      }
      catch (ItemNotFoundException e)
      {
      }
   }

   public void testSetNamespacePrefix() throws RepositoryException
   {
      try
      {
         session.setNamespacePrefix("nt", "http://www.jcp.org/jcr/1.0");
         fail();
      }
      catch (NamespaceException e)
      {
      }
   }
}