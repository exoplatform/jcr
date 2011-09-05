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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.container.component.ComponentPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AddNamespacePluginHolder is used in NamespaceRegistryImpl for registration 
 * namespaces from xml-configuration after repository start. 
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: AddNamespacePluginHolder.java 111 2010-11-11 11:11:11Z tolusha $
 */
public class AddNamespacePluginHolder
{
   private final List<ComponentPlugin> addNamespacesPlugins;

   /**
    * AddNamespacePluginHolder constructor.
    * 
    * @param componentPlugins
    *          list of AddNamespacesPlugins
    */
   public AddNamespacePluginHolder(List<ComponentPlugin> componentPlugins)
   {
      this.addNamespacesPlugins = new ArrayList<ComponentPlugin>(componentPlugins);
   }

   /**
    * @return unmodifiable list of AddNamespacesPlugins
    */
   public List<ComponentPlugin> getAddNamespacesPlugins()
   {
      return Collections.unmodifiableList(addNamespacesPlugins);
   }
}
