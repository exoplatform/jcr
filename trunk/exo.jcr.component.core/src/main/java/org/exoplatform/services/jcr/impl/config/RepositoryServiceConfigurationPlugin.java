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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.config;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;

/**
 * This class allows us to add new {@link RepositoryServiceConfiguration} thanks to the component plugins
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 28 sept. 2009  
 */
public class RepositoryServiceConfigurationPlugin extends BaseComponentPlugin
{

   private final String confPath;
   
   public RepositoryServiceConfigurationPlugin(InitParams params)
   {
      ValueParam param = params == null ? null : params.getValueParam("conf-path");
      if (param == null || param.getValue() == null || param.getValue().trim().length() == 0)
      {
         throw new IllegalArgumentException("The value-param 'conf-path' is mandatory, please check your configuration");
      }
      else
      {
         this.confPath = param.getValue().trim();
      }
   }

   /**
    * @return the path of the configuration file to retrieve
    */
   public String getConfPath()
   {
      return confPath;
   }
}
