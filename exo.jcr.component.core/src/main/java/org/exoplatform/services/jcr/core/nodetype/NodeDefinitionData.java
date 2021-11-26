/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.core.nodetype;

import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by The eXo Platform SAS <br>
 *
 * The NodeDefinitionData class extends ItemDefinitionValue with the addition of getter methods
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: NodeDefinitionData.java 25471 2008-12-19 08:34:01Z ksm $
 * @LevelAPI Unsupported
 */

public class NodeDefinitionData extends ItemDefinitionData
{
   protected final InternalQName[] requiredPrimaryTypes;

   protected final InternalQName defaultPrimaryType;

   protected final boolean allowsSameNameSiblings;

   public NodeDefinitionData(InternalQName name, InternalQName declaringNodeType, boolean autoCreated,
      boolean mandatory, int onParentVersion, boolean protectedItem, InternalQName[] requiredPrimaryTypes,
      InternalQName defaultPrimaryType, boolean allowsSameNameSiblings)
   {
      super(name, declaringNodeType, autoCreated, mandatory, onParentVersion, protectedItem);
      this.requiredPrimaryTypes = requiredPrimaryTypes;
      this.defaultPrimaryType = defaultPrimaryType;
      this.allowsSameNameSiblings = allowsSameNameSiblings;
      Arrays.sort(requiredPrimaryTypes, new Comparator<InternalQName>()
      {

         public int compare(InternalQName o1, InternalQName o2)
         {
            return o1.getAsString().compareTo(o2.getAsString());
         }
      });
   }

   public InternalQName[] getRequiredPrimaryTypes()
   {
      return requiredPrimaryTypes;
   }

   public InternalQName getDefaultPrimaryType()
   {
      return defaultPrimaryType;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (allowsSameNameSiblings ? 1231 : 1237);
      result = prime * result + ((defaultPrimaryType == null) ? 0 : defaultPrimaryType.hashCode());
      result = prime * result + Arrays.hashCode(requiredPrimaryTypes);
      return result;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (!super.equals(obj))
      {
         return false;
      }
      if (!(obj instanceof NodeDefinitionData))
      {
         return false;
      }
      NodeDefinitionData other = (NodeDefinitionData)obj;
      if (allowsSameNameSiblings != other.allowsSameNameSiblings)
      {
         return false;
      }
      if (defaultPrimaryType == null)
      {
         if (other.defaultPrimaryType != null)
         {
            return false;
         }
      }
      else if (!defaultPrimaryType.equals(other.defaultPrimaryType))
      {
         return false;
      }
      if (!Arrays.deepEquals(requiredPrimaryTypes, other.requiredPrimaryTypes))
      {
         return false;
      }
      return true;
   }

   public boolean isAllowsSameNameSiblings()
   {
      return allowsSameNameSiblings;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeDefinition()
   {
      return true;
   }
}
