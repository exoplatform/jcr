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
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.RegistryInitializationEntryPlugin");

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
