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
package org.exoplatform.services.jcr.ext.hierarchy.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 2:49:30 PM
 */
public class AddPathPlugin extends BaseComponentPlugin
{

   private HierarchyConfig paths;

   private String description;

   private String name;

   public AddPathPlugin(InitParams params)
   {
      paths = (HierarchyConfig)params.getObjectParamValues(HierarchyConfig.class).get(0);
   }

   public HierarchyConfig getPaths()
   {
      return paths;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String s)
   {
      name = s;
   }

   public String getDescription()
   {
      return description;
   }

   public void setDescription(String s)
   {
      description = s;
   }

}
