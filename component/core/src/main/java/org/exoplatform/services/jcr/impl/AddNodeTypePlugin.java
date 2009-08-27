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
package org.exoplatform.services.jcr.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;

public class AddNodeTypePlugin
   extends BaseComponentPlugin
{

   /**
    * We have list of node types in order of its adding
    */
   private Map<String, List<String>> nodeTypes = new LinkedHashMap<String, List<String>>();

   public static final String AUTO_CREATED = "autoCreatedInNewRepository";

   public AddNodeTypePlugin(InitParams params)
   {

      Iterator<ValuesParam> vparams = params.getValuesParamIterator();
      while (vparams.hasNext())
      {
         ValuesParam nodeTypeParam = vparams.next();
         nodeTypes.put(nodeTypeParam.getName(), nodeTypeParam.getValues());
      }
   }

   public List<String> getNodeTypesFiles(String repositoryName)
   {
      return nodeTypes.get(repositoryName);
   }
}
