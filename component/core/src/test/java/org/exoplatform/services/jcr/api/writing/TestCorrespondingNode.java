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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestCorrespondingNode.java 13891 2008-05-05 16:02:30Z
 *          pnedonosko $
 */
public class TestCorrespondingNode extends JcrAPIBaseTest
{

   public void setUp() throws Exception
   {
      super.setUp();

      // TODO
      // if(!((RepositoryImpl) repository).isWorkspaceInitialized("ws2"));
      // ((RepositoryImpl) repository).initWorkspace("ws2", "nt:unstructured");

   }

   public void testDifferentWs() throws RepositoryException
   {

   }

   public void testCorrespondingPath() throws RepositoryException
   {
      Session session2 = repository.login(credentials, "ws2");
      Node root = session2.getRootNode();
      Node file = root.addNode("testCorrespondingPath", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session2.save();

      // root = session.getRootNode();
      // root.addNode("test", "nt:unstructured");
      // session.save();

      workspace.clone("ws2", "/testCorrespondingPath", "/testCorrespondingPath1", false);

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node node1 = (Node)session.getItem("/testCorrespondingPath1/childNode2/jcr:content");
      //System.out.println(">> "+((SessionImpl)session2).getWorkspaceDataContainer
      // ());
      assertEquals("/testCorrespondingPath/childNode2/jcr:content", node1.getCorrespondingNodePath("ws2"));

      session.getRootNode().getNode("testCorrespondingPath1").remove();
      session.save();
      session2.getRootNode().getNode("testCorrespondingPath").remove();
      session2.save();

   }

   public void testNodeUpdate() throws RepositoryException
   {

      Session session2 = repository.login(credentials, "ws2");
      Node root = session2.getRootNode();
      Node file = root.addNode("testNodeUpdate", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session2.save();

      workspace.clone("ws2", "/testNodeUpdate", "/testNodeUpdate1", false);

      session = (SessionImpl)repository.login(credentials, WORKSPACE);

      assertEquals(((Node)session.getItem("/testNodeUpdate1/childNode2/jcr:content")).getUUID(), ((Node)session2
         .getItem("/testNodeUpdate/childNode2/jcr:content")).getUUID());

      Node node2 = (Node)session2.getItem("/testNodeUpdate/childNode2/jcr:content");
      Value bv = session.getValueFactory().createValue("this is the NEW content", PropertyType.BINARY);
      // log.debug("BV >>>>>> "+bv.getString());
      node2.setProperty("jcr:data", bv);

      // node2.setProperty("jcr:data", new
      // BinaryValue("this is the NEW content"));
      assertEquals("this is the NEW content", node2.getProperty("jcr:data").getString());
      node2 = (Node)session2.getItem("/testNodeUpdate");

      node2.save();

      Node node1 = (Node)session.getItem("/testNodeUpdate1/childNode2/jcr:content");
      // log.debug(">>> DATA "+node1.getProperty("jcr:data"));

      assertEquals("this is the content", node1.getProperty("jcr:data").getString());

      node1.update("ws2");

      // because of bug
      node1 = (Node)session.getItem("/testNodeUpdate1/childNode2/jcr:content");

      assertEquals("this is the NEW content", node1.getProperty("jcr:data").getString());
      // No needs in save()

      Session session = repository.login(credentials, "ws");
      node1 = (Node)session.getItem("/testNodeUpdate1/childNode2/jcr:content");
      assertEquals("this is the NEW content", node1.getProperty("jcr:data").getString());

      session.getRootNode().getNode("testNodeUpdate1").remove();
      session.save();
      session2.getRootNode().getNode("testNodeUpdate").remove();
      session2.save();
   }

   /*
    * public void testOrderingChild() throws RepositoryException {
    * fail("Ordering Child TODO!"); }
    */
}
