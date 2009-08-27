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
package org.exoplatform.services.jcr.usecases.common;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 09.07.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestNodeGetRequiredPrimaryTypes.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestNodeGetRequiredPrimaryTypes
   extends BaseUsecasesTest
{

   public void testRootNode() throws Exception
   {

      Node rootNode = session.getRootNode();
      NodeDefinition rnDefinition = rootNode.getDefinition();
      try
      {
         NodeType[] requiredPrimaryTypes = rnDefinition.getRequiredPrimaryTypes();
         assertNotNull("Root node: NodeDefinition.getRequiredPrimaryTypes() must not be null", requiredPrimaryTypes);
      }
      catch (Exception e)
      {
         fail("Root node: Error of NodeDefinition.getRequiredPrimaryTypes() call: " + e.getMessage());
      }
   }

   public void testUnstructuredNode() throws Exception
   {

      Node testUnstructured = session.getRootNode().addNode("testUnstructured", "nt:unstructured");
      session.save();
      NodeDefinition rnDefinition = testUnstructured.getDefinition();
      try
      {
         NodeType[] requiredPrimaryTypes = rnDefinition.getRequiredPrimaryTypes();
         assertNotNull("NodeDefinition.getRequiredPrimaryTypes() must not be null", requiredPrimaryTypes);
      }
      catch (Exception e)
      {
         fail("Error of NodeDefinition.getRequiredPrimaryTypes() call: " + e.getMessage());
      }
   }
}
