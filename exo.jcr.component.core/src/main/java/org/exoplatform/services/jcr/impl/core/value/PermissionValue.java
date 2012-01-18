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
import org.exoplatform.services.security.IdentityConstants;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.jcr.ValueFormatException;

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

   public PermissionValue(ValueData data) throws IOException
   {
      super(TYPE, data);

      try
      {
         String[] persArray = parse(new String(data.getAsByteArray()));
         this.identity = persArray[0];
         this.permission = persArray[1];
      }
      catch (IOException e)
      {
         throw new RuntimeException("FATAL ERROR IOException occured: " + e.getMessage(), e);
      }
   }

   public PermissionValue(String identity, String permission) throws IOException
   {
      super(TYPE, new TransientValueData(asString(identity, permission))); // identity + " " +
      // permission
      if (identity != null && identity.indexOf(" ") != -1)
         throw new RuntimeException("Identity should not contain ' '");
      if (permission != null && !permission.equals(PermissionType.READ) && !permission.equals(PermissionType.ADD_NODE)
         && !permission.equals(PermissionType.REMOVE) && !permission.equals(PermissionType.SET_PROPERTY))
         throw new RuntimeException("Permission should be one of defined in PermissionType. Have " + permission);
      this.identity = identity;
      this.permission = permission;
   }

   static public PermissionValue parseValue(String pstring) throws IOException
   {
      String[] persArray = parse(pstring);
      return new PermissionValue(persArray[0], persArray[1]);
   }

   static public String[] parse(String pstring)
   {
      StringTokenizer parser = new StringTokenizer(pstring, AccessControlEntry.DELIMITER);
      String identityString = parser.nextToken();
      String permissionString = parser.nextToken();

      String[] persArray = new String[2];

      if (identityString != null)
      {
         persArray[0] = identityString;
      }
      else
      {
         persArray[0] = IdentityConstants.ANY;
      }
      if (permissionString != null)
      {
         persArray[1] = permissionString;
      }
      else
      {
         persArray[1] = PermissionType.READ;
      }
      return persArray;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getInternalString() throws ValueFormatException
   {
      return asString(identity, permission);
   }

   static protected String asString(String identity, String permission)
   {
      if (identity != null || permission != null) // SystemIdentity.ANY, PermissionType.ALL
         return (identity != null ? identity : IdentityConstants.ANY) + AccessControlEntry.DELIMITER
            + (permission != null ? permission : PermissionType.READ);
      else
         return "";
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
