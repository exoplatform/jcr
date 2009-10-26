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

import org.exoplatform.services.jcr.api.observation.SimpleListener;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.pham@exoplatform.com
 * phamvuxuanhoa@yahoo.com Jul 3, 2006
 */
public class TestNodeTypeRegister extends BaseUsecasesTest
{

   public void testRegisterNodeType() throws Exception
   {
      Session session = repository.getSystemSession(repository.getSystemWorkspaceName());
      NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
      NodeTypeValue nodeTypeValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      nodeTypeValue.setName("exo:testNodeType");
      nodeTypeValue.setPrimaryItemName("");
      nodeTypeValue.setDeclaredSupertypeNames(superType);
      ExtendedNodeTypeManager extNodeTypeManager = (ExtendedNodeTypeManager)nodeTypeManager;
      try
      {
         nodeTypeManager.getNodeType("exo:testNodeType");
         fail("Node Type is registed");
      }
      catch (Exception e)
      {
      }

      try
      {
         extNodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      }
      catch (NullPointerException e)
      {
         fail("something wrong and registerNodeType() throws NullPointException");
         e.printStackTrace();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      try
      {
         NodeType nodeType = nodeTypeManager.getNodeType("exo:testNodeType");
         assertNotNull(nodeType);
      }
      catch (Exception e)
      {
      }

   }

   public void testRegisterNodeType2() throws Exception
   {
      Session session = repository.getSystemSession(repository.getSystemWorkspaceName());

      SimpleListener listener = new SimpleListener("testSessionOpen", log, 0);
      session.getWorkspace().getObservationManager().addEventListener(listener, Event.NODE_ADDED, root.getPath(),
         false, null, null, false);

      NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
      NodeTypeValue nodeTypeValue = new NodeTypeValue();

      nodeTypeValue.setName("exo:testNodeType2");
      nodeTypeValue.setMixin(false);
      nodeTypeValue.setOrderableChild(false);
      nodeTypeValue.setPrimaryItemName("");
      List<String> superTypeNames = new ArrayList<String>();
      superTypeNames.add("nt:base");
      nodeTypeValue.setDeclaredSupertypeNames(superTypeNames);
      nodeTypeValue.setPrimaryItemName("");

      ExtendedNodeTypeManager extNodeTypeManager = repositoryService.getRepository().getNodeTypeManager();
      try
      {
         nodeTypeManager.getNodeType("exo:testNodeType2");
         fail("Node Type is registed");
      }
      catch (Exception e)
      {
      }

      try
      {
         extNodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      }
      catch (NullPointerException e)
      {
         fail("something wrong and registerNodeType() throws NullPointException");
         e.printStackTrace();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      session.save();

      try
      {
         NodeType nodeType = nodeTypeManager.getNodeType("exo:testNodeType2");
         assertNotNull(nodeType);
      }
      catch (Exception e)
      {
      }

   }
}
