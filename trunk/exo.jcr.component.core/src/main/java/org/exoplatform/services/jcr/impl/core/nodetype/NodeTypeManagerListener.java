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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * The <code>NodeTypeRegistryListener</code> interface allows an implementing object to be informed
 * about node type (un)registration.
 * 
 * @see NodeTypeRegistry#addListener(NodeTypeRegistryListener)
 * @see NodeTypeRegistry#removeListener(NodeTypeRegistryListener)
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
