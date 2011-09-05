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
import org.exoplatform.services.jcr.impl.Constants;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: ItemDefinitionData.java 26474 2009-01-09 16:15:25Z ksm $
 */
public class ItemDefinitionData
{

   protected final InternalQName name;

   protected final InternalQName declaringNodeType;

   protected final boolean autoCreated;

   protected final boolean mandatory;

   protected final int onParentVersion;

   protected final boolean protectedItem;

   public ItemDefinitionData(InternalQName name, InternalQName declaringNodeType, boolean autoCreated,
      boolean mandatory, int onParentVersion, boolean protectedItem)
   {
      this.name = name;
      this.declaringNodeType = declaringNodeType;
      this.autoCreated = autoCreated;
      this.mandatory = mandatory;
      this.onParentVersion = onParentVersion;
      this.protectedItem = protectedItem;
   }

   public boolean isResidualSet()
   {
      return this.getName().equals(Constants.JCR_ANY_NAME);
   }

   public InternalQName getName()
   {
      return name;
   }

   public InternalQName getDeclaringNodeType()
   {
      return declaringNodeType;
   }

   public boolean isAutoCreated()
   {
      return autoCreated;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + (autoCreated ? 1231 : 1237);
      result = prime * result + ((declaringNodeType == null) ? 0 : declaringNodeType.hashCode());
      result = prime * result + (mandatory ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + onParentVersion;
      result = prime * result + (protectedItem ? 1231 : 1237);
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
      if (obj == null)
      {
         return false;
      }
      if (!(obj instanceof ItemDefinitionData))
      {
         return false;
      }
      ItemDefinitionData other = (ItemDefinitionData)obj;
      if (autoCreated != other.autoCreated)
      {
         return false;
      }
      if (declaringNodeType == null)
      {
         if (other.declaringNodeType != null)
         {
            return false;
         }
      }
      else if (!declaringNodeType.equals(other.declaringNodeType))
      {
         return false;
      }
      if (mandatory != other.mandatory)
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
      if (onParentVersion != other.onParentVersion)
      {
         return false;
      }
      if (protectedItem != other.protectedItem)
      {
         return false;
      }
      return true;
   }

   public boolean isMandatory()
   {
      return mandatory;
   }

   public int getOnParentVersion()
   {
      return onParentVersion;
   }

   public boolean isProtected()
   {
      return protectedItem;
   }

   @Override
   public String toString()
   {
      return name.getAsString();
   }

}
