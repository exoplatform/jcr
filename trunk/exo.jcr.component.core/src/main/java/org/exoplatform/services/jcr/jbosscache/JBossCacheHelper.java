/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: ISPNCacheHelper.java 111 2010-11-11 11:11:11Z tolusha $
 *
 */
public class JBossCacheHelper extends TemplateConfigurationHelper
{

   /**
    * Creates configuration cache helper with pre-configured for JBossCache parameters,<br>
    * including: "jbosscache-*" and "jgroups-configuration", and excluding "jbosscache-configuration"
    * 
    * @param cfm
    *          instance for looking up resources
    */
   public JBossCacheHelper(ConfigurationManager cfm)
   {
      super(new String[]{"^jbosscache-.*", "^jgroups-configuration"}, new String[]{"^jbosscache-configuration"}, cfm);
   }
}
