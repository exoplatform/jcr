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

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: AddNamespacesPlugin.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class AddNamespacesPlugin extends BaseComponentPlugin
{

   private Map<String, String> namespaces = new HashMap<String, String>();

   public AddNamespacesPlugin(InitParams params)
   {
      PropertiesParam param = params.getPropertiesParam("namespaces");

      if (param != null)
      {
         namespaces = param.getProperties();
      }
   }

   public Map<String, String> getNamespaces()
   {
      return namespaces;
   }

}
