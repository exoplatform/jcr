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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;

import javax.jcr.nodetype.NodeTypeIterator;

public class TestItemDefinitionsHolder extends JcrImplBaseTest
{
   private final boolean isImplemented = false;

   private boolean isLoaded = false;

   private NodeTypeDataManager holder;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      if (isImplemented)
      {
         isLoaded = true;
      }
      else if (!isImplemented && !isLoaded)
      {
         NodeTypeManagerImpl ntManager = ((NodeTypeManagerImpl)repository.getNodeTypeManager());
         NodeTypeIterator nodeTypes = ntManager.getAllNodeTypes();
         holder = session.getWorkspace().getNodeTypesHolder();
         // while (nodeTypes.hasNext()) {
         // NodeType type = nodeTypes.nextNodeType();
         // holder.putDefinitions((ExtendedNodeType) type);
         // }
         isLoaded = true;
      }

   }

   public void testNodeDefinition() throws Exception
   {

      if (isLoaded)
      {
         NodeDefinitionData def1 = holder.findChildNodeDefinition(Constants.JCR_CONTENT, Constants.NT_FILE,

         Constants.NT_BASE);
         NodeDefinitionData def2 = holder.findChildNodeDefinition(Constants.JCR_CONTENT, Constants.NT_FILE,

         Constants.NT_RESOURCE);

         assertNotNull(def1);
         assertNotNull(def2);
         assertEquals(def1, def2);
         assertEquals(Constants.JCR_CONTENT, def1.getName());

         assertNull(holder.findChildNodeDefinition(Constants.JCR_DEFAULTPRIMNARYTYPE, Constants.NT_FILE,
            Constants.NT_RESOURCE));
      }
   }

   public void testResidualNodeDefinition() throws Exception
   {

      //System.out.println(">>>>>>>>>>>> "+holder.getChildNodeDefinition(Constants
      // .NT_FILE,
      // new InternalQName(null, "test"), Constants.NT_UNSTRUCTURED).getName());
      if (isLoaded)
      {
         NodeDefinitionData def1 =
            holder.findChildNodeDefinition(new InternalQName(null, "test"), Constants.NT_UNSTRUCTURED,

            Constants.NT_UNSTRUCTURED);
         NodeDefinitionData def2 =
            holder.findChildNodeDefinition(new InternalQName(Constants.NS_EXO_URI, "test11111"),
               Constants.NT_UNSTRUCTURED,

               Constants.NT_FILE);

         assertNotNull(def1);
         assertNotNull(def2);
         assertEquals(def1, def2);
         assertEquals(Constants.JCR_ANY_NAME, def1.getName());
      }
   }

}
