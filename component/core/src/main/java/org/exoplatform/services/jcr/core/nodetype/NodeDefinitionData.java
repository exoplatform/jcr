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
 * Created by The eXo Platform SAS. <br/>Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeDefinitionData.java 34801 2009-07-31 15:44:50Z dkatayev $
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
   }

   public InternalQName[] getRequiredPrimaryTypes()
   {
      return requiredPrimaryTypes;
   }

   public InternalQName getDefaultPrimaryType()
   {
      return defaultPrimaryType;
   }

   public boolean isAllowsSameNameSiblings()
   {
      return allowsSameNameSiblings;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if ((obj == null) || (obj.getClass() != this.getClass()))
         return false;
      // object must be Test at this point
      NodeDefinitionData test = (NodeDefinitionData)obj;
      return defaultPrimaryType == test.defaultPrimaryType && allowsSameNameSiblings == test.allowsSameNameSiblings
         && super.equals(test) && Arrays.equals(this.requiredPrimaryTypes, test.requiredPrimaryTypes);
   }
}
