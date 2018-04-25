/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Group;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: GroupImpl.java 76870 2011-11-22 10:38:54Z dkuleshov $
 */
public class GroupImpl implements Group, ExtendedCloneable
{

   /**
    * The group's description.
    */
   private String description;

   /**
    * The group's id. It is have a form /ancestor/parent/groupname.
    */
   private String groupId;

   /**
    * The group's name.
    */
   private String groupName;

   /**
    * The group's label.
    */
   private String label;

   /**
    * The group's parentId. It is have a form /ancestor/parent.
    */
   private String parentId;

   /**
    * The internal group identifier.
    */
   private String internalId;

   /**
    * GroupImpl constructor.
    */
   GroupImpl()
   {
   }

   /**
    * GroupImpl constructor.
    */
   GroupImpl(String name, String parentId)
   {
      setGroupName(name);
      setParentId(parentId);
   }

   /**
    * {@inheritDoc}
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * {@inheritDoc}
    */
   public String getGroupName()
   {
      return groupName;
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return groupId;
   }

   /**
    * {@inheritDoc}
    */
   public String getLabel()
   {
      return label;
   }

   /**
    * {@inheritDoc}
    */
   public String getParentId()
   {
      return parentId;
   }

   /**
    * Returns internal group identifier.
    */
   String getInternalId()
   {
      return internalId;
   }

   /**
    * {@inheritDoc}
    */
   public void setDescription(String desc)
   {
      description = desc;
   }

   /**
    * {@inheritDoc}
    */
   public void setGroupName(String name)
   {
      groupName = name;
      groupId = (parentId == null ? "" : parentId) + "/" + groupName;
   }

   /**
    * Sets new parenId and refresh groupId.
    */
   public void setParentId(String parentId)
   {
      this.parentId = (parentId == null || parentId.equals("") ? null : parentId);
      setGroupName(groupName);
   }

   /**
    * Sets internal group identifier.  
    */
   void setInternalId(String internalId)
   {
      this.internalId = internalId;
   }

   /**
    * {@inheritDoc}
    */
   public void setLabel(String name)
   {
      label = name;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[groupId=" + getId() + "][groupName=" + getGroupName() + "][parentId=" + getParentId() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public GroupImpl clone()
   {
      try
      {
         return (GroupImpl)super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }
   }
}
