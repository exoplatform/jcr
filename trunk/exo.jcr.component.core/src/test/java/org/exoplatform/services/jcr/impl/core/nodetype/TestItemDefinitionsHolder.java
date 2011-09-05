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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;

public class TestItemDefinitionsHolder extends AbstractNodeTypeTest
{

   public void testNodeDefinition() throws Exception
   {

      NodeDefinitionData def1 = nodeTypeDataManager.getChildNodeDefinition(Constants.JCR_CONTENT, Constants.NT_FILE,

      Constants.NT_BASE);
      NodeDefinitionData def2 = nodeTypeDataManager.getChildNodeDefinition(Constants.JCR_CONTENT, Constants.NT_FILE,

      Constants.NT_RESOURCE);

      assertNotNull(def1);
      assertNotNull(def2);
      assertEquals(def1, def2);
      assertEquals(Constants.JCR_CONTENT, def1.getName());

      assertNull(nodeTypeDataManager.getChildNodeDefinition(Constants.JCR_DEFAULTPRIMNARYTYPE, Constants.NT_FILE,
         Constants.NT_RESOURCE));

   }

   public void testResidualNodeDefinition() throws Exception
   {

      NodeDefinitionData def1 =
         nodeTypeDataManager.getChildNodeDefinition(new InternalQName(null, "test"), Constants.NT_UNSTRUCTURED,

         Constants.NT_UNSTRUCTURED);
      NodeDefinitionData def2 =
         nodeTypeDataManager.getChildNodeDefinition(new InternalQName(Constants.NS_EXO_URI, "test11111"),
            Constants.NT_UNSTRUCTURED,

            Constants.NT_FILE);

      assertNotNull(def1);
      assertNotNull(def2);
      assertEquals(def1, def2);
      assertEquals(Constants.JCR_ANY_NAME, def1.getName());

   }

}
