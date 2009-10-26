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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestSession.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestSession extends JcrAPIBaseTest
{

   public void testSave() throws RepositoryException
   {
      Node root = session.getRootNode();
      try
      {
         root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:propertyDefinition");
         session.save();
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      try
      {
         root.getNode("childNode/childNode2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      Node node = root.addNode("nodeType", "nt:base");
      session.save();
      root.getNode("nodeType").remove();
      session.save();
   }

   public void testRefresh() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.addNode("nodeType", "exo:mockNodeType");
      node.addNode("jcr:childNodeDefinition", "nt:childNodeDefinition");
      session.refresh(false);
      try
      {
         root.getNode("nodeType");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      session.save();

      // Test refresh(true)
      // ///////////////////
   }

   public void testHasPendingChanges() throws RepositoryException
   {
      assertFalse(session.hasPendingChanges());
      session.getRootNode().addNode("test", "nt:unstructured");
      assertTrue(session.hasPendingChanges());
      session.save();
      assertFalse(session.hasPendingChanges());
   }

   public void testSaveWithUUID() throws RepositoryException
   {
   }

   public void testPropertiesManipThenSave() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.addNode("testPropertiesManipThenSave", "nt:unstructured");
      node.addNode("node2BRem", "nt:unstructured");
      node.setProperty("existingProp", "existingValue");
      node.setProperty("existingProp2", "existingValue2");
      session.save();
      node.setProperty("prop", "propValue");
      node.setProperty("existingProp", "existingValueBis");
      node.getProperty("existingProp2").remove();
      node.getNode("node2BRem").remove();
      node.addNode("addedNode", "nt:unstructured");
      session.save();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      node = session.getRootNode().getNode("testPropertiesManipThenSave");
      root = session.getRootNode();
      try
      {
         node.getProperty("prop");
      }
      catch (PathNotFoundException e)
      {
         e.printStackTrace();
         fail("exception should not be thrown");
      }
      assertEquals("existingValueBis", node.getProperty("existingProp").getString());
      try
      {
         node.getProperty("existingProp2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      try
      {
         node.getNode("node2BRem");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      node.getNode("addedNode");

      // System.out.println("REMOVE childNode");
      root.getNode("testPropertiesManipThenSave").remove();
      // System.out.println("SAVE childNode");
      session.save();
      // System.out.println("REMOVED");
   }

}
