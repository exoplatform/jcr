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
package org.exoplatform.services.xml.resolving.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class AddXMLResolvingContextPlugin extends BaseComponentPlugin
{

   private Map<String, String> publicIDs_ = new HashMap<String, String>();

   private Map<String, String> systemIDs_ = new HashMap<String, String>();

   public AddXMLResolvingContextPlugin(InitParams params)
   {
      if (params != null)
      {
         Iterator<PropertiesParam> iterator = params.getPropertiesParamIterator();
         while (iterator.hasNext())
         {
            PropertiesParam propertiesParam = iterator.next();
            String uri = propertiesParam.getProperty("uri");
            String publicId = propertiesParam.getProperty("publicId");
            String systemId = propertiesParam.getProperty("systemId");
            if (publicId != null && uri != null)
               publicIDs_.put(publicId, uri);
            if (systemId != null && uri != null)
               systemIDs_.put(systemId, uri);
         }
      }
   }

   public Map<String, String> getPublicIDsResolvingtable()
   {
      return publicIDs_;
   }

   public Map<String, String> getSystemIDsResolvingtable()
   {
      return systemIDs_;
   }

}
