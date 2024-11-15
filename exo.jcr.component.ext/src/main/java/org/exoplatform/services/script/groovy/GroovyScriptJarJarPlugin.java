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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A plugin that retrieves a mapping from the init param named <i>mapping</i>. The param
 * is a multivalued string, each string has the format {@literal left->right} where left and right
 * are package full qualified names.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class GroovyScriptJarJarPlugin extends BaseComponentPlugin
{

   /** The mapping state. */
   private final Map<String, String> mapping = new HashMap<String, String>();

   /** Our logger. */
   private final static Log LOG = ExoLogger.getLogger("exo.core.component.script.groovy.GroovyScriptJarJarPlugin");

   public GroovyScriptJarJarPlugin(InitParams params)
   {

      List<String> values = params.getValuesParam("mapping").getValues();

      if (mapping == null)
      {
         LOG.warn("Was expecting a mapping init param");
      }
      else
      {
         for (Iterator<String> i = values.iterator(); i.hasNext();)
         {
            String rule = i.next();

            String[] tmp = rule.split("\\-\\>");
            if (tmp.length == 2)
            {
               String left = tmp[0].trim();
               String right = tmp[1].trim();
               mapping.put(left, right);
               LOG.debug("Added mapping rule " + left + " -> " + right);
            }
            else
            {
               LOG.warn("Malformed mapping rule:" + rule);
            }
         }
      }
   }

   public Map<String, String> getMapping()
   {
      return mapping;
   }
}
