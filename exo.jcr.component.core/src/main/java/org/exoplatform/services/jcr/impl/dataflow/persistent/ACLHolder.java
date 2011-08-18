/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ACLHolder
{
   /**
    * The id of the node that holds some ACL info
    */
   private final String id;
   
   /**
    * A flag indicating whether or not the node has owner set
    */
   private boolean owner;
   
   /**
    * A flag indicating whether or not the node has permissions set
    */
   private boolean permissions;

   public ACLHolder(String id)
   {
      this.id = id;
   }

   /**
    * @return the id
    */
   public String getId()
   {
      return id;
   }

   /**
    * @return the owner
    */
   public boolean hasOwner()
   {
      return owner;
   }

   /**
    * @return the permissions
    */
   public boolean hasPermissions()
   {
      return permissions;
   }

   /**
    * @param owner the owner to set
    */
   public void setOwner(boolean owner)
   {
      this.owner = owner;
   }

   /**
    * @param permissions the permissions to set
    */
   public void setPermissions(boolean permissions)
   {
      this.permissions = permissions;
   }
}
