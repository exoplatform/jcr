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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;

public class TestCopyDataVisitor extends JcrImplBaseTest
{

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void testCopyInWorkspace() throws Exception
   {

      Node root = session.getRootNode();
      Node file = root.addNode("testCopy", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      workspace.copy("/testCopy", "/testCopy1");

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      assertNotNull(session.getItem("/testCopy1"));
      assertNotNull(session.getItem("/testCopy1/childNode2"));
      assertNotNull(session.getItem("/testCopy1/childNode2/jcr:content"));
      assertNotNull(session.getItem("/testCopy"));

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();
      try
      {
         workspace.copy("/toCorrupt", "/test/childNode/corrupted");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      session.getRootNode().getNode("testCopy1").remove();
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("existNode").remove();
      session.getRootNode().getNode("testCopy").remove();
      session.save();

   }

}
