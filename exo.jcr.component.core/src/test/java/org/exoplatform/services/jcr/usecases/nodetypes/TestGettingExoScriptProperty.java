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
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeUtil;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestGettingExoScriptProperty.java 111 2010-11-11 11:11:11Z rainf0x $
 */
public class TestGettingExoScriptProperty
   extends BaseUsecasesTest
{
   public void testLoadProperyDef3() throws Exception
   {
      NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();

      NodeType nt = manager.getNodeType("exo:addMetadataAction");

      PropertyDefinition propDef = null;
      PropertyDefinition[] propsDef = nt.getPropertyDefinitions();
      
      assertEquals(8, propsDef.length);

      for (PropertyDefinition pDef : propsDef)
      {
         if (pDef.getName().equals("exo:script"))
         {
            propDef = pDef;
         }
      }

      // will never happen since at least jcr:primaryType of nt:base accomplish the request
      if (propDef == null)
      {
         throw new NotExecutableException("No protected property def found.");
      }

      NodeType nodeType = propDef.getDeclaringNodeType();
      Value value = NodeTypeUtil.getValueOfType(session, propDef.getRequiredType());

      assertFalse("canSetProperty(String propertyName, Value value) must "
               + "return false if the property is protected.", nodeType.canSetProperty(propDef.getName(), value));
   }
}
