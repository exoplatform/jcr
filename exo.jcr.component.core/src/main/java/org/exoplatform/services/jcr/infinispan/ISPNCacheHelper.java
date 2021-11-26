/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: ISPNCacheHelper.java 111 2010-11-11 11:11:11Z tolusha $
 *
 */
public class ISPNCacheHelper extends TemplateConfigurationHelper
{

   /**
    * Creates configuration cache helper with pre-configured for Infinispan cache parameters,<br>
    * including: "infinispan-*" and "jgroups-configuration", and excluding "infinispan-configuration"
    * 
    * @param cfm
    *          instance for looking up resources
    */
   public ISPNCacheHelper(ConfigurationManager cfm)
   {
      super(new String[]{"^jgroups-configuration", "^infinispan-.*"}, new String[]{"^infinispan-configuration"}, cfm);
   }

}
