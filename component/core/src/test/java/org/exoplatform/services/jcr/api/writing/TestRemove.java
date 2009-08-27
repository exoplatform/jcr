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

import java.util.Calendar;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestRemove.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestRemove
   extends JcrAPIBaseTest
{

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node file = root.addNode("TestRemove", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      Node u = root.addNode("u", "nt:unstructured");
      u.setProperty("prop", "val");
      session.save();
   }

   public void tearDown() throws Exception
   {
      Node root = session.getRootNode();
      root.getNode("TestRemove").remove();
      root.getNode("u").remove();
      session.save();

      super.tearDown();
   }

   public void testRemove() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node c = root.getNode("TestRemove/childNode2");
      // log.debug("CHILD NODE>>> "+c);
      root.getNode("TestRemove/childNode2").remove();
      // c = root.getNode("childNode/childNode2");
      // log.debug("CHILD NODE>>> "+c+" "+((NodeImpl)c).getState());

      // log.debug("CHILD NODE>>> "+root.getNode("childNode").getNodes().nextNode().getPath());
      // log.debug("CHILD NODE>>> "+root.getNode("childNode").getNode("childNode2").getPath());

      assertFalse(root.getNode("TestRemove").hasNodes());
      session.save();
      //log.debug(">>> "+((ItemLocation)((NodeImpl)session.getItem("/childNode")).getChildNodeLocations
      // ().get(0)).getPath());

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      try
      {
         root.getNode("TestRemove/childNode2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
   }

   public void testSetNullValuedProperty() throws RepositoryException
   {

      // log.debug(">>> set null value ");
      root.setProperty("prop", (Value) null);
      try
      {
         PropertyImpl p = (PropertyImpl) root.getProperty("prop");
         System.out.println("Removed property ====== " + p.getData().getQPath().getAsString());
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
   }

   public void testRemoveProperty() throws RepositoryException
   {

      Node root = session.getRootNode();

      try
      {
         root.getProperty("TestRemove/childNode2/jcr:content/jcr:data").remove();
         // session.save();
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
         root.refresh(false);
      }
      root.getProperty("u/prop").remove();
      root.save();

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      // System.out.println(">>"+session.getContainer());

      try
      {
         root.getProperty("u/prop");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

   }

   public void testInvalidItemStateException() throws RepositoryException
   {
      Node root = session.getRootNode();

      Node subRoot = root.getNode("u");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      subRoot.save();

      subRoot.getNode("child").remove();

      Session session2 = repository.login(credentials, "ws");
      Node n2 = session2.getRootNode().getNode("u/child");
      n2.remove();

      session.save();

      try
      {
         // log.debug("start save >>>");
         session2.save();
         fail("InvalidItemStateException should have been thrown");
      }
      catch (InvalidItemStateException e)
      {
      }

   }

   public void testRemoveRferencedNode() throws RepositoryException
   {
      Node root = session.getRootNode();

      Node testNode = root.addNode("testRemoveRferencedNode", "nt:unstructured");
      testNode.addMixin("mix:referenceable");

      // Should be saved first
      root.save();

      Node n1 = root.addNode("n1", "nt:unstructured");
      Node n2 = root.addNode("n2", "nt:unstructured");

      n1.setProperty("p1", testNode);
      n2.setProperty("p1", testNode);

      root.save();

      try
      {
         testNode.remove();
         // testNode.save();can't do so
         session.save();
         fail("ReferentialIntegrityException should have been thrown");
      }
      catch (ReferentialIntegrityException e)
      {
         session.refresh(false);
      }

      Session session2 = repository.login(credentials, "ws");
      try
      {
         session2.getItem("/testRemoveRferencedNode").remove();
         session2.save();
         fail("ReferentialIntegrityException should have been thrown");
      }
      catch (ReferentialIntegrityException e)
      {
         session2.refresh(false);
      }

      // allowed here
      n2.remove();
      n1.remove();
      testNode.remove();
      session.save();
   }

   public void testRemoveSameNameSibs() throws RepositoryException
   {
      Node root = session.getRootNode();

      Node subRoot = root.getNode("u");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");
      root.save();
      root.getNode("u/child[3]");
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove();
      log.debug(">>>> SAME NAME end " + session.getTransientNodesManager().dump());

      root.save();
      log.debug("SIZE >>>" + root.getNode("u").getNodes().getSize()); // /child[2]");
      log.debug("SIZE >>>" + session.getRootNode().getNode("u").getNodes().getSize()); // /child[2]");

      assertEquals(2, subRoot.getNodes().getSize());
      try
      {
         root.getNode("u/child[3]");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

   }

}
