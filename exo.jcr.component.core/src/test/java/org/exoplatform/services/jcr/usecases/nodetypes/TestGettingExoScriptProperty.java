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
package org.exoplatform.services.jcr.usecases.nodetypes;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestGettingExoScriptProperty.java 111 2010-11-11 11:11:11Z rainf0x $
 */
public class TestGettingExoScriptProperty
   extends BaseUsecasesTest
{
   
   public static final int ANY_PROPERTY_TYPE = -1;
   
   public void testLoadPropertyDef() throws Exception
   {
     PropertyDefinition propDef = locatePropertyDef(session, ANY_PROPERTY_TYPE, false, true, false, false);
      
      System.out.println("\n\n" +propDef.getName() + "\n\n");
      
      assertEquals(true, propDef.isProtected());
      
      if (propDef.getName().equals("exo:script")) {
         assertEquals(false, propDef.isProtected());
      }
   }
   
   public void testExoScriptProperty() throws Exception
   {
      Node rootNode = session.getRootNode();
      
      Node action = rootNode.addNode("123", "exo:scriptAction");
      
      action.setProperty( "exo:name", "exo_name__value");
      action.setProperty( "exo:lifecyclePhase", "add");
      action.setProperty( "exo:roles", new String[] {"*"});
      
      action.setProperty( "exo:script", "exo_script__value");
      session.save();
      
      PropertyDefinition[] propsDef = action.getPrimaryNodeType().getDeclaredPropertyDefinitions();
      
      for (PropertyDefinition pDef : propsDef)
      {
         if (pDef.getName().equals("exo:script")) {
            
            assertEquals(pDef.isProtected(), false);
            return;
         }
      }
      
      fail("The property definition is not found.");
   }
   
   public void testGetPropDefFromNTM() throws Exception
   {
      NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
      
      NodeType nt = manager.getNodeType("exo:scriptAction");
      
      PropertyDefinition[] propsDef = nt.getDeclaredPropertyDefinitions();
      
      for (PropertyDefinition pDef : propsDef)
      {
         if (pDef.getName().equals("exo:script")) {
            assertEquals(pDef.isProtected(), false);
            return;
         }
      }
      
      fail("The property definition is not found.");
      
   }

   public static PropertyDefinition locatePropertyDef(Session session, int propertyType, boolean multiple,
            boolean isProtected, boolean constraints, boolean residual) throws RepositoryException
   {

      NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
      NodeTypeIterator types = manager.getAllNodeTypes();

      while (types.hasNext())
      {
         NodeType type = types.nextNodeType();
         PropertyDefinition propDefs[] = type.getDeclaredPropertyDefinitions();
         for (int i = 0; i < propDefs.length; i++)
         {
            PropertyDefinition propDef = propDefs[i];
            
            System.out.println("\n\n " + propDef.getName());

            if (propertyType != ANY_PROPERTY_TYPE && propDef.getRequiredType() != propertyType)
            {
               continue;
            }

            if (propertyType == ANY_PROPERTY_TYPE && propDef.getRequiredType() == PropertyType.UNDEFINED)
            {
               continue;
            }

            if (multiple && !propDef.isMultiple())
            {
               continue;
            }
            if (!multiple && propDef.isMultiple())
            {
               continue;
            }

            System.out.println("isProtected && !propDef.isProtected() ==" + (isProtected && !propDef.isProtected()));
            if (isProtected && !propDef.isProtected())
            {
               continue;
            }
            if (!isProtected && propDef.isProtected())
            {
               continue;
            }

            String vc[] = propDef.getValueConstraints();
            if (!constraints && vc != null && vc.length > 0)
            {
               continue;
            }
            if (constraints)
            {
               // property def with constraints requested
               if (vc == null || vc.length == 0)
               {
                  // property def has no constraints
                  continue;
               }
            }

            if (!residual && propDef.getName().equals("*"))
            {
               continue;
            }

            if (residual && !propDef.getName().equals("*"))
            {
               continue;
            }

            // also skip property residual property definition if there
            // is another residual definition
            if (residual)
            {
               // check if there is another residual property def
               if (getNumResidualPropDefs(type) > 1)
               {
                  continue;
               }
            }

            if (!residual)
            {
               // if not looking for a residual property def then there
               // must not be any residual definition at all on the node
               // type
               if (getNumResidualPropDefs(type) > 0)
               {
                  continue;
               }
            }

            return propDef;
         }
      }
      return null;
   }
   
   private static int getNumResidualPropDefs(NodeType type) {
      PropertyDefinition[] pDefs = type.getPropertyDefinitions();
      int residuals = 0;
      for (int j = 0; j < pDefs.length; j++) {
          PropertyDefinition pDef = pDefs[j];
          if (pDef.getName().equals("*")) {
              residuals++;
          }
      }
      return residuals;
  }
}
