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
package org.exoplatform.services.jcr.webdav.utils;

import org.exoplatform.common.util.HierarchicalProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class XmlUtils
{

   public static Map<QName, WebDavProperty> parsePropStat(HierarchicalProperty response)
   {
      HashMap<QName, WebDavProperty> properties = new HashMap<QName, WebDavProperty>();

      for (HierarchicalProperty propStat : response.getChildren())
      {
         if (!propStat.getName().equals(new QName("DAV:", "propstat")))
         {
            continue;
         }

         HierarchicalProperty prop = propStat.getChild(new QName("DAV:", "prop"));
         HierarchicalProperty stat = propStat.getChild(new QName("DAV:", "status"));

         int status = new Integer(stat.getValue().split(" ")[1]).intValue();

         List<HierarchicalProperty> props = prop.getChildren();
         for (HierarchicalProperty property : props)
         {
            WebDavProperty webDavProperty = new WebDavProperty(property);
            webDavProperty.setStatus(status);
            properties.put(webDavProperty.getName(), webDavProperty);
         }
      }

      return properties;
   }

}
