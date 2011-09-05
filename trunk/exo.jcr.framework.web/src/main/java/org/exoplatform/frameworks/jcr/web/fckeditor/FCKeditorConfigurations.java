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
package org.exoplatform.frameworks.jcr.web.fckeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains the configuration settings for the FCKEditor.<br>
 * Adding element to this collection you can override the settings specified in the config.js file.
 * 
 * @author Simone Chiaretta (simo@users.sourceforge.net)
 */
public class FCKeditorConfigurations extends HashMap
{

   /**
    * Initialize the configuration collection
    */
   public FCKeditorConfigurations()
   {
      super();
   }

   /**
    * Generate the url parameter sequence used to pass this configuration to the editor.
    * 
    * @return html endocode sequence of configuration parameters
    */
   public String getUrlParams()
   {
      StringBuffer osParams = new StringBuffer();

      for (Iterator i = this.entrySet().iterator(); i.hasNext();)
      {
         Map.Entry entry = (Map.Entry)i.next();
         if (entry.getValue() != null)
            osParams.append("&" + encodeConfig(entry.getKey().toString()) + "="
               + encodeConfig(entry.getValue().toString()));
      }
      return osParams.toString();
   }

   private String encodeConfig(String txt)
   {
      txt = txt.replaceAll("&", "%26");
      txt = txt.replaceAll("=", "%3D");
      txt = txt.replaceAll("\"", "%22");
      return txt;
   }

}
