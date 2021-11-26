/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * The <code>NodeTypeRegistryListener</code> interface allows an implementing object to be informed
 * about node type (un)registration.
 * 
 * @see NodeTypeDataManager#addListener(NodeTypeManagerListener)
 * @see NodeTypeDataManager#removeListener(NodeTypeManagerListener)
 * @LevelAPI Unsupported
 */
public interface NodeTypeManagerListener
{

   /**
    * Called when a node type has been registered.
    * 
    * @param ntName
    *          name of the node type that has been registered
    */
   void nodeTypeRegistered(InternalQName ntName);

   /**
    * Called when a node type has been re-registered.
    * 
    * @param ntName
    *          name of the node type that has been registered
    */
   void nodeTypeReRegistered(InternalQName ntName);

   /**
    * Called when a node type has been deregistered.
    * 
    * @param ntName
    *          name of the node type that has been unregistered
    */
   void nodeTypeUnregistered(InternalQName ntName);
}
