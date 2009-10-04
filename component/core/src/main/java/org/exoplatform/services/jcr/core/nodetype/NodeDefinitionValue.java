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
package org.exoplatform.services.jcr.core.nodetype;

import org.exoplatform.services.jcr.impl.core.nodetype.NodeDefinitionImpl;

import java.util.Arrays;
import java.util.List;

import javax.jcr.nodetype.NodeDefinition;

/**
 * Created by The eXo Platform SAS.<br/> NodeDefinition value object
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: NodeDefinitionValue.java 11907 2008-03-13 15:36:21Z ksm $
 */

public final class NodeDefinitionValue extends ItemDefinitionValue
{

   private String defaultNodeTypeName;

   private List<String> requiredNodeTypeNames;

   private boolean sameNameSiblings;

   public NodeDefinitionValue()
   {
   }

   /**
    * @param name
    * @param autoCreate
    * @param mandatory
    * @param onVersion
    * @param readOnly
    * @param defaultNodeTypeName
    * @param requiredNodeTypeNames
    * @param sameNameSiblings
    */
   public NodeDefinitionValue(String name, boolean autoCreate, boolean mandatory, int onVersion, boolean readOnly,
      String defaultNodeTypeName, List<String> requiredNodeTypeNames, boolean sameNameSiblings)
   {
      super(name, autoCreate, mandatory, onVersion, readOnly);
      this.defaultNodeTypeName = defaultNodeTypeName;
      this.requiredNodeTypeNames = requiredNodeTypeNames;
      this.sameNameSiblings = sameNameSiblings;
   }

   public NodeDefinitionValue(NodeDefinition nodeDefinition)
   {
      super(nodeDefinition);
      this.defaultNodeTypeName = ((NodeDefinitionImpl)nodeDefinition).getDefaultPrimaryTypeName();
      this.requiredNodeTypeNames = Arrays.asList(((NodeDefinitionImpl)nodeDefinition).getRequiredPrimaryTypeNames());
      this.sameNameSiblings = nodeDefinition.allowsSameNameSiblings();
   }

   /**
    * @return Returns the defaultNodeTypeName.
    */
   public String getDefaultNodeTypeName()
   {
      return defaultNodeTypeName;
   }

   /**
    * @param defaultNodeTypeName The defaultNodeTypeName to set.
    */
   public void setDefaultNodeTypeName(String defaultNodeTypeName)
   {
      this.defaultNodeTypeName = defaultNodeTypeName;
   }

   /**
    * @return Returns the sameNameSiblings.
    */
   public boolean isSameNameSiblings()
   {
      return sameNameSiblings;
   }

   /**
    * @param sameNameSiblings The sameNameSiblings to set.
    */
   public void setSameNameSiblings(boolean multiple)
   {
      this.sameNameSiblings = multiple;
   }

   /**
    * @return Returns the requiredNodeTypeNames.
    */
   public List<String> getRequiredNodeTypeNames()
   {
      return requiredNodeTypeNames;
   }

   /**
    * @param requiredNodeTypeNames The requiredNodeTypeNames to set.
    */
   public void setRequiredNodeTypeNames(List<String> requiredNodeTypeNames)
   {
      this.requiredNodeTypeNames = requiredNodeTypeNames;
   }
}
