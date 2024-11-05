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
package org.exoplatform.services.script.groovy;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Should be used by third part service for loading script at start.
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class GroovyScriptURLLoaderPlugin extends BaseComponentPlugin
{

   private List<String> urls;

   public GroovyScriptURLLoaderPlugin(InitParams params)
   {
      if (params != null)
      {
         ObjectParameter param = params.getObjectParam("scripts");
         if (param != null)
            urls = ((GroovyScriptURLs)param.getObject()).getUrls();
      }
   }

   /**
    * @return list of URL from configuration.
    */
   public List<String> getUrls()
   {
      return urls;
   }

   /**
    * Should be used in configuration as object parameter.
    */
   public static class GroovyScriptURLs
   {

      private List<String> urls = new ArrayList<String>();

      public List<String> getUrls()
      {
         return urls;
      }

      public void setUrls(List<String> urls)
      {
         this.urls = urls;
      }
   }

}
