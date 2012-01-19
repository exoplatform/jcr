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
package org.exoplatform.services.jcr.usecases.nodetypes;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.jcr.Node;

/**
 * 
 * HOW-TO: It's not simply to run this test, as we need differents in nodes UUIDs in data container.
 * 
 * Testing with source distribution. Run test once (like 'mvn clean test') its create repository and
 * register node types. Change source file MultiDbJDBCConnection.java in FIND_NODES_BY_PARENTID
 * variable use " order by I.ID DESC" instead " order by I.ID ". Be careful to revert changes back
 * after test. And run the repository software again on existed and initialized database (like 'mvn
 * test'). On the second phase you'll see repository startup printout with messages like: '>>> Node
 * types registration cycle X started' '<<< Node types registration cycle X finished' where X is
 * cycle number of node types registration. More one cycle can be. It's depends on nodetypes count
 * referenced one-by-one with forward declaration of dependent type.
 * 
 * Another way to perform test it's change order of nodes returned by data container on
 * getChildNodesData(). This can be done by direct change of UUIDs in the data storage (database).
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestNodeTypeRegisterReferenced.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestNodeTypeRegisterReferenced extends BaseUsecasesTest
{

   public void testRegisterNodeTypesRelated() throws Exception
   {

      byte[] xmlData = readXmlContent("/org/exoplatform/services/jcr/usecases/nodetypes/nodetypes-usecase-test.xml");

      ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlData);

      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeTypes(xmlInput, 0, NodeTypeDataManager.TEXT_XML);

      String ntName = "exojcrtest:testNodeType__1";
      assertNotNull(ntManager.getNodeType(ntName));
      Node ntRoot = (Node)repository.getSystemSession().getItem(NodeTypeManagerImpl.NODETYPES_ROOT);
      assertTrue(ntRoot.hasNode(ntName));
      session.getRootNode().addNode("test1", ntName);
      session.save();

      ntName = "exojcrtest:testNodeType__2";
      assertNotNull(ntManager.getNodeType(ntName));
      ntRoot = (Node)repository.getSystemSession().getItem(NodeTypeManagerImpl.NODETYPES_ROOT);
      assertTrue(ntRoot.hasNode(ntName));
      session.getRootNode().addNode("test2", ntName);
      session.save();

      ntName = "exojcrtest:testNodeType__3";
      assertNotNull(ntManager.getNodeType(ntName));
      ntRoot = (Node)repository.getSystemSession().getItem(NodeTypeManagerImpl.NODETYPES_ROOT);
      assertTrue(ntRoot.hasNode(ntName));
      Node test3 = session.getRootNode().addNode("test3", ntName);
      test3.addNode("somePrimaryItem", "exojcrtest:testNodeType_required");
      session.save();

      ntName = "exojcrtest:testNodeType__4";
      assertNotNull(ntManager.getNodeType(ntName));
      ntRoot = (Node)repository.getSystemSession().getItem(NodeTypeManagerImpl.NODETYPES_ROOT);
      assertTrue(ntRoot.hasNode(ntName));
      Node test4 = session.getRootNode().addNode("test4", ntName);
      test4.addNode("somePrimaryItem", "exojcrtest:testNodeType_required");
      session.save();
   }

   private byte[] readXmlContent(String fileName)
   {

      try
      {
         InputStream is = TestNodeTypeRegisterReferenced.class.getResourceAsStream(fileName);
         ByteArrayOutputStream output = new ByteArrayOutputStream();

         int r = is.available();
         byte[] bs = new byte[r];
         while (r > 0)
         {
            r = is.read(bs);
            if (r > 0)
            {
               output.write(bs, 0, r);
            }
            r = is.available();
         }
         is.close();
         return output.toByteArray();
      }
      catch (Exception e)
      {
         log.error("Error read file '" + fileName + "' with NodeTypes. Error:" + e);
         return null;
      }
   }
}
