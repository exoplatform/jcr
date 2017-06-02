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
import org.exoplatform.services.organization.MembershipType;

import java.util.Date;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: MembershipTypeImpl.java 76870 2011-11-22 10:38:54Z dkuleshov $
 */
public class MembershipTypeImpl implements MembershipType, ExtendedCloneable
{

   /**
    * The description of the membership type.
    */
   private String description;

   /**
    * The name of the membership type
    */
   private String name;

   /**
    * The internal identifier.
    */
   private String internalId;

   /**
    * MembershipTypeImpl constructor.
    */

   /**
    * The created date.
    */
   private Date createdDate;

   /**
    * The modified date.
    */
   private Date modifiedDate;


   MembershipTypeImpl()
   {
   }

   /**
    * MembershipTypeImpl constructor.
    */
   MembershipTypeImpl(String internalId, String name, String description)
   {
      this.internalId = internalId;
      this.name = name;
      this.description = description;
   }

   /**
    * {@inheritDoc}
    */
   public Date getCreatedDate()
   {
      return createdDate;
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
   public Date getModifiedDate()
   {
      return modifiedDate;
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    */
   public String getOwner()
   {
      return null;
   }

   /**
    * Returns the internal identifier.
    */
   String getInternalId()
   {
      return internalId;
   }

   /**
    * Set internal identifier of the membership type.
    */
   void setInternalId(String internalId)
   {
      this.internalId = internalId;
   }

   /**
    * {@inheritDoc}
    */
   public void setCreatedDate(Date d)
   {
      this.createdDate =d;
   }

   /**
    * {@inheritDoc}
    */
   public void setDescription(String s)
   {
      description = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setModifiedDate(Date d)
   {
      this.modifiedDate =d;
   }

   /**
    * {@inheritDoc}
    */
   public void setName(String s)
   {
      name = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setOwner(String s)
   {
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[type=" + getName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public MembershipTypeImpl clone()
   {
      try
      {
         return (MembershipTypeImpl)super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }
   }
}
