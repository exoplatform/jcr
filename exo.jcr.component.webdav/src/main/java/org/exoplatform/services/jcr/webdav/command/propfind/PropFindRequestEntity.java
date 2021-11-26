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

package org.exoplatform.services.jcr.webdav.command.propfind;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class PropFindRequestEntity
{

   /**
    * Request body.
    */
   protected HierarchicalProperty input;

   /**
    * Constructor.
    * 
    * @param input request body
    */
   public PropFindRequestEntity(HierarchicalProperty input)
   {
      this.input = input;
   }

   /**
    * Returns the type of request.
    * 
    * @return request type
    */
   public String getType()
   {

      if (input == null)
      {
         return "allprop";
      }

      if (input.getChild(PropertyConstants.DAV_ALLPROP_INCLUDE) != null)
      {
         return "include";
      }

      QName name = input.getChild(0).getName();
      if (name.getNamespaceURI().equals("DAV:"))
         return name.getLocalPart();
      else
         return null;
   }

}
