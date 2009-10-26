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
package org.exoplatform.services.jcr.ext.registry;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:vitaly.parfonov@gmail.com">Vitaly Parfonov</a>
 * @version $Id: $
 */
public class RegistryInitializationEntryPlugin extends BaseComponentPlugin
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger(RegistryInitializationEntryPlugin.class);

   private HashMap<String, String> appConfiguration = new HashMap<String, String>();

   private String location;

   public RegistryInitializationEntryPlugin(InitParams initParams)
   {

      if (initParams != null)
      {
         PropertiesParam properties = initParams.getPropertiesParam("locations");
         if (properties != null)
            location = properties.getProperty("group-path");
      }
      if (initParams != null)
      {
         Iterator<ValueParam> iterator = initParams.getValueParamIterator();
         while (iterator.hasNext())
         {
            ValueParam valueParam = (ValueParam)iterator.next();
            appConfiguration.put(valueParam.getName(), valueParam.getValue().trim());
         }
      }
   }

   public HashMap<String, String> getAppConfiguration()
   {
      return appConfiguration;
   }

   public String getLocation()
   {
      return location;
   }

}
