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
package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;

import java.io.IOException;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * 
 * @version $Id: PermissionValue.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class PermissionValue extends BaseValue
{

   private static final int TYPE = ExtendedPropertyType.PERMISSION;

   private String identity;

   private String permission;

   /**
    * PermissionValue constructor.
    */
   public PermissionValue(ValueData data)
   {
      super(TYPE, data);

      try
      {
         AccessControlEntry accessEntry = ValueDataUtil.getPermission(data);

         this.identity = accessEntry.getIdentity();
         this.permission = accessEntry.getPermission();
      }
      catch (RepositoryException e)
      {
         throw new RuntimeException("FATAL ERROR RepositoryException occured: " + e.getMessage(), e);
      }
   }

   /**
    * PermissionValue constructor.
    */
   public PermissionValue(String identity, String permission) throws IOException
   {
      super(TYPE, new TransientValueData(new AccessControlEntry(identity, permission)));

      if (permission != null && !permission.equals(PermissionType.READ) && !permission.equals(PermissionType.ADD_NODE)
         && !permission.equals(PermissionType.REMOVE) && !permission.equals(PermissionType.SET_PROPERTY))
      {
         throw new RuntimeException("Permission should be one of defined in PermissionType. Have " + permission);
      }

      this.identity = identity;
      this.permission = permission;
   }

   /**
    * @return Returns the identity.
    */
   public String getIdentity()
   {
      return identity;
   }

   /**
    * @return Returns the permission
    */
   public String getPermission()
   {
      return permission;
   }
}
