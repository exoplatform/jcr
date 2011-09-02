/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.ItemData;

/**
 * This class allows other class to be notified when a given cache event occurs
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface WorkspaceStorageCacheListener
{
   /**
    * Called when a cache entry corresponding to the given item has been added
    * @param data the item corresponding to the added cache entry
    */
   void onCacheEntryAdded(ItemData data);
   
   /**
    * Called when a cache entry corresponding to the given item has been updated
    * @param data the item corresponding to the updated cache entry
    */
   void onCacheEntryUpdated(ItemData data);
}
