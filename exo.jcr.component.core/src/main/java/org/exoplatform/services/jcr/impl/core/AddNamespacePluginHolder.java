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
