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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.infinispan.CacheKey;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Store QPath as key in cache.
 * 
 * 15.06.07
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheQPath.java 3393 2010-11-04 07:54:54Z tolusha $
 */
class CacheQPath extends CacheKey
{
   public CacheQPath()
   {
      super();
   }

   CacheQPath(String ownerId, String parentId, QPath path, ItemType itemType)
   {
      this(ownerId, parentId, path.getEntries()[path.getEntries().length - 1], itemType);
   }

   CacheQPath(String ownerId, String parentId, QPathEntry name, ItemType itemType)
   {
      super(ownerId, new StringBuilder().append(parentId != null ? parentId : Constants.ROOT_PARENT_UUID)
         .append(name.getAsString(true)).append(itemType.toString()).toString(), parentId);
   }
}
