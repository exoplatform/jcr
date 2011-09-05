/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestWorksapceSamenameNodeCopy.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestWorkspaceSamenameNodeCopy extends BaseUsecasesTest
{
   private String parentNodeName = null;

   public void testWorksapceSamenameNodeCopy() throws Exception
   {
      parentNodeName = "myNode";

      // add node to ws workspace
      Session sessionWS = (SessionImpl)repository.login(credentials, "ws");
      sessionWS.getRootNode().addNode("myNode", "nt:unstructured");
      sessionWS.save();
      assertTrue(sessionWS.itemExists("/myNode"));
      // copy node to another workspase

      Session sessionWS2 = (SessionImpl)repository.login(credentials, "ws2");
      sessionWS2.getWorkspace().copy("ws", "/myNode", "/myNode");

      assertTrue(sessionWS2.itemExists("/myNode"));
   }

   public void tearDown() throws Exception
   {
      if (parentNodeName != null)
      {
         // clean up workspaces
         Session sessionWS = (SessionImpl)repository.login(credentials, "ws");
         Item item = sessionWS.getItem("/" + parentNodeName);
         item.remove();
         sessionWS.save();

         Session sessionWS2 = (SessionImpl)repository.login(credentials, "ws2");
         item = sessionWS2.getItem("/" + parentNodeName);
         item.remove();
         sessionWS2.save();
      }
      parentNodeName = null;
      super.tearDown();
   }

   public void testWorksapceSamenameNodeCopy2() throws Exception
   {
      parentNodeName = "parent";
      // add node to ws workspace
      Session sessionWS = (SessionImpl)repository.login(credentials, "ws");
      Node parent1 = sessionWS.getRootNode().addNode("parent");
      parent1.addNode("myNode", "nt:unstructured");
      sessionWS.save();
      assertTrue(sessionWS.itemExists("/parent/myNode"));
      // copy node to another workspase

      Session sessionWS2 = (SessionImpl)repository.login(credentials, "ws2");
      Node parent2 = sessionWS2.getRootNode().addNode("parent");
      sessionWS2.save();
      sessionWS2.getWorkspace().copy("ws", "/parent/myNode", "/parent/myNode");

      assertTrue(sessionWS2.itemExists("/parent/myNode"));
   }

   public void testWorksapceSamenameNodeCopyWithSubNodes() throws Exception
   {
      parentNodeName = "myNode";

      // add node to ws workspace
      Session sessionWS = (SessionImpl)repository.login(credentials, "ws");
      Node n = sessionWS.getRootNode().addNode("myNode", "nt:unstructured");
      n.addNode("subnode", "nt:unstructured");
      sessionWS.save();
      assertTrue(sessionWS.itemExists("/myNode"));
      assertTrue(sessionWS.itemExists("/myNode/subnode"));
      // copy node to another workspase

      Session sessionWS2 = (SessionImpl)repository.login(credentials, "ws2");
      sessionWS2.getWorkspace().copy("ws", "/myNode", "/myNode");

      assertTrue(sessionWS2.itemExists("/myNode"));
      assertTrue(sessionWS2.itemExists("/myNode/subnode"));
   }
}
