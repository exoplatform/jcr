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

package org.exoplatform.services.jcr.webdav.utils;

import org.exoplatform.common.util.HierarchicalProperty;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class WebDavProperty extends HierarchicalProperty
{

   private int status;

   // public WebDavProperty(String name, String value) {
   // super(name, value);
   // }
   //  
   public WebDavProperty(QName name, String value)
   {
      super(name, value);
   }

   public WebDavProperty(QName name)
   {
      super(name);
   }

   public WebDavProperty(HierarchicalProperty property)
   {
      super(property.getName(), property.getValue());
      for (HierarchicalProperty child : property.getChildren())
      {
         addChild(child);
      }
   }

   public void setStatus(int status)
   {
      this.status = status;
   }

   public int getStatus()
   {
      return status;
   }

}
