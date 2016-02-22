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

import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.Arrays;

/**
 * Created by The eXo Platform SAS. Define base abstraction for NodeType data
 * used in core. <br>
 * Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeData.java 27741 2009-02-02 16:59:51Z ksm $
 */

public class NodeTypeDataImpl implements NodeTypeData
{
   protected InternalQName name;

   protected InternalQName primaryItemName;

   protected InternalQName[] declaredSupertypeNames;

   protected PropertyDefinitionData[] declaredPropertyDefinitions;

   protected NodeDefinitionData[] declaredChildNodeDefinitions;

   protected Boolean hasOrderableChildNodes;

   protected Boolean mixin;

   //  protected Boolean isAbstract;

   //   /**
   //    * Default true.
   //    */
   //   protected boolean isQueryable;

   public NodeTypeDataImpl(InternalQName name, InternalQName primaryItemName, boolean mixin,
      boolean hasOrderableChildNodes, InternalQName[] declaredSupertypeNames,
      PropertyDefinitionData[] declaredPropertyDefinitions, NodeDefinitionData[] declaredChildNodeDefinitions)
   {

      this.name = name;
      this.primaryItemName = primaryItemName;
      this.mixin = mixin;
      this.hasOrderableChildNodes = hasOrderableChildNodes;
      //this.isAbstract = isAbstract;
      //this.isQueryable = isQueryable;
      this.declaredSupertypeNames = declaredSupertypeNames;
      this.declaredPropertyDefinitions = declaredPropertyDefinitions;
      this.declaredChildNodeDefinitions = declaredChildNodeDefinitions;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#getDeclaredChildNodeDefinitions()
    */
   public NodeDefinitionData[] getDeclaredChildNodeDefinitions()
   {
      return declaredChildNodeDefinitions;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#getDeclaredPropertyDefinitions()
    */
   public PropertyDefinitionData[] getDeclaredPropertyDefinitions()
   {
      return declaredPropertyDefinitions;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(declaredChildNodeDefinitions);
      result = prime * result + Arrays.hashCode(declaredPropertyDefinitions);
      result = prime * result + Arrays.hashCode(declaredSupertypeNames);
      result = prime * result + (hasOrderableChildNodes ? 1231 : 1237);
      result = prime * result + (mixin ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((primaryItemName == null) ? 0 : primaryItemName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (getClass() != obj.getClass())
      {
         return false;
      }
      NodeTypeDataImpl other = (NodeTypeDataImpl)obj;
      if (!Arrays.deepEquals(declaredChildNodeDefinitions, other.declaredChildNodeDefinitions))
      {
         return false;
      }
      if (!Arrays.deepEquals(declaredPropertyDefinitions, other.declaredPropertyDefinitions))
      {
         return false;
      }
      if (!Arrays.deepEquals(declaredSupertypeNames, other.declaredSupertypeNames))
      {
         return false;
      }
      if (hasOrderableChildNodes != other.hasOrderableChildNodes)
      {
         return false;
      }
      if (mixin != other.mixin)
      {
         return false;
      }
      if (name == null)
      {
         if (other.name != null)
         {
            return false;
         }
      }
      else if (!name.equals(other.name))
      {
         return false;
      }
      if (primaryItemName == null)
      {
         if (other.primaryItemName != null)
         {
            return false;
         }
      }
      else if (!primaryItemName.equals(other.primaryItemName))
      {
         return false;
      }
      return true;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#getDeclaredSupertypeNames()
    */
   public InternalQName[] getDeclaredSupertypeNames()
   {
      return declaredSupertypeNames;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#getPrimaryItemName()
    */
   public InternalQName getPrimaryItemName()
   {
      return primaryItemName;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#getName()
    */
   public InternalQName getName()
   {
      return name;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#hasOrderableChildNodes()
    */
   public boolean hasOrderableChildNodes()
   {
      return hasOrderableChildNodes;
   }

   /**
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#isMixin()
    */
   public boolean isMixin()
   {
      return mixin;
   }

   //   /**
   //    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeData#isQueryable()
   //    */
   //   public boolean isQueryable()
   //   {
   //      return isQueryable;
   //   }

}
