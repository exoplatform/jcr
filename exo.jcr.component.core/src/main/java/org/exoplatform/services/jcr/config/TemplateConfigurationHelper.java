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
package org.exoplatform.services.jcr.config;

import org.exoplatform.container.configuration.ConfigurationManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds configuration from template using map of template-variables -- value.
 * Class provides extra functionality for filtering parameters by pattern, excluding 
 * unnecessary parameters. 
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TemplateConfigurationHelper.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TemplateConfigurationHelper extends org.exoplatform.container.util.TemplateConfigurationHelper
{

   /**
    * Creates instance of template configuration helper with given lists of filtering 
    * patterns. Parameter will be included only if it matches any include-pattern and
    * doesn't match any exclude-pattern. I.e. You can include "extended-*" and exclude
    * "extended-type". Please refer to Java regexp documentation. Filtering for this
    * example, should be defined as following:
    * include: "^extended-.*"
    * exclude: "^extended-type"
    * 
    * @param includes Array with string representation of include reg-exp patterns
    * @param excludes Array with string representation of exclude reg-exp patterns
    * @param cfm instance for looking up resources
    */
   public TemplateConfigurationHelper(String[] includes, String[] excludes, ConfigurationManager cfm)
   {
      super(includes, excludes, cfm);
   }

   /**
    * Reads configuration file from a stream and replaces all the occurrences of template-variables 
    * (like : "${parameter.name}") with values provided in the map. 
    * 
    * @param inputStream
    * @param parameters
    * @return
    * @throws IOException
    */
   public InputStream fillTemplate(InputStream inputStream, Collection<SimpleParameterEntry> parameters) throws IOException
   {
      Map<String, String> map = new HashMap<String, String>();
      for (SimpleParameterEntry parameterEntry : parameters)
      {
         map.put(parameterEntry.getName(), parameterEntry.getValue());
      }
      return fillTemplate(inputStream, map);
   }

   /**
    * Reads configuration file from file-system and replaces all the occurrences of template-variables 
    * (like : "${parameter.name}") with values provided in the map. 
    * 
    * @param filename
    * @param parameters
    * @return
    * @throws IOException
    */
   public InputStream fillTemplate(String filename, Collection<SimpleParameterEntry> parameters) throws IOException
   {
      Map<String, String> map = new HashMap<String, String>();
      for (SimpleParameterEntry parameterEntry : parameters)
      {
         map.put(parameterEntry.getName(), parameterEntry.getValue());
      }
      return fillTemplate(filename, map);
   }
}
