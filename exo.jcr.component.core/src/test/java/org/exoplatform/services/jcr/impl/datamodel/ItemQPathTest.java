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
package org.exoplatform.services.jcr.impl.datamodel;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS
 * 
 * 08.02.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ItemQPathTest.java 14508 2008-05-20 10:07:45Z ksm $
 */
public class ItemQPathTest extends JcrImplBaseTest
{

   protected final String TEST_ROOT = "qpath_test";

   protected NodeImpl testRoot = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = (NodeImpl)session.getRootNode().addNode(TEST_ROOT);
      session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {

      testRoot.remove();
      session.save();

      super.tearDown();
   }

   public void testSameNameSibling() throws Exception
   {

      testRoot.addNode("node", "nt:base");
      session.save();
      Node file = testRoot.addNode("node", "nt:file");
      Node nContent = file.addNode("jcr:content", "nt:unstructured");
      nContent.setProperty("currenTime", Calendar.getInstance());
      nContent.setProperty("info", "Info string");
      Node resource = nContent.addNode("fileData", "nt:resource");
      resource.setProperty("jcr:mimeType", "text/plain");
      resource.setProperty("jcr:lastModified", Calendar.getInstance());
      resource.setProperty("jcr:data", "DATA STRING");
      session.save();

      SessionImpl user =
         (SessionImpl)repository.login(this.credentials /* session.getCredentials() */, session.getWorkspace()
            .getName());

      try
      {
         NodeImpl n1 = (NodeImpl)user.getRootNode().getNode(TEST_ROOT).getNode("node");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node", n1.getPath());
         NodeImpl n2 = (NodeImpl)user.getRootNode().getNode(TEST_ROOT).getNode("node[2]");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node[2]", n2.getPath());
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }
   }

   public void testSameNameSiblingBulkAdd() throws Exception
   {

      testRoot.addNode("node", "nt:base");
      session.save();
      Node file = testRoot.addNode("node", "nt:file");
      Node nContent = file.addNode("jcr:content", "nt:unstructured");
      nContent.setProperty("currenTime", Calendar.getInstance());
      nContent.setProperty("info", "Info string");
      Node resource = nContent.addNode("fileData", "nt:resource");
      resource.setProperty("jcr:mimeType", "text/plain");
      resource.setProperty("jcr:lastModified", Calendar.getInstance());
      resource.setProperty("jcr:data", "DATA STRING");
      testRoot.addNode("node", "nt:base");
      testRoot.addNode("node", "nt:base");
      session.save();

      SessionImpl user =
         (SessionImpl)repository.login(this.credentials /* session.getCredentials() */, session.getWorkspace()
            .getName());

      try
      {
         Node n1 = user.getRootNode().getNode(TEST_ROOT).getNode("node");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node", n1.getPath());
         Node n2 = user.getRootNode().getNode(TEST_ROOT).getNode("node[2]");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node[2]", n2.getPath());
         Node n3 = user.getRootNode().getNode(TEST_ROOT).getNode("node[3]");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node[3]", n3.getPath());
         Node n4 = user.getRootNode().getNode(TEST_ROOT).getNode("node[4]");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node[4]", n4.getPath());
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }
   }

   public void testSameNameSiblingRemove() throws Exception
   {

      Node n1 = testRoot.addNode("node", "nt:base");
      session.save();

      Node file = testRoot.addNode("node", "nt:file");
      Node nContent = file.addNode("jcr:content", "nt:unstructured");
      nContent.setProperty("currenTime", Calendar.getInstance());
      nContent.setProperty("info", "Info string");
      Node resource = nContent.addNode("fileData", "nt:resource");
      resource.setProperty("jcr:mimeType", "text/plain");
      resource.setProperty("jcr:lastModified", Calendar.getInstance());
      resource.setProperty("jcr:data", "DATA STRING");

      Node n3 = testRoot.addNode("node", "nt:base");
      Node n4 = testRoot.addNode("node", "nt:base");
      n4.addMixin("mix:referenceable");
      String n4Uuid = n4.getUUID();
      session.save();

      SessionImpl user =
         (SessionImpl)repository.login(this.credentials /* session.getCredentials() */, session.getWorkspace()
            .getName());

      try
      {
         user.getRootNode().getNode(TEST_ROOT).getNode("node").remove();
         user.getRootNode().getNode(TEST_ROOT).getNode("node[2]").remove(); // n3
         user.save();

         Node un1 = user.getRootNode().getNode(TEST_ROOT).getNode("node");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node", un1.getPath());
         Node content = un1.getNode("jcr:content");
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node/jcr:content", content.getPath());

         Node un2 = user.getRootNode().getNode(TEST_ROOT).getNode("node[2]"); // user.getRootNode().
         // getNode
         // (TEST_ROOT).getNodes()
         assertEquals("Path is invalid", "/" + TEST_ROOT + "/node[2]", un2.getPath());
         try
         {
            assertEquals("UUID is invalid", n4Uuid, un2.getUUID());
         }
         catch (UnsupportedRepositoryOperationException e)
         {
            fail(e.getMessage());
         }

      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }
   }
}
