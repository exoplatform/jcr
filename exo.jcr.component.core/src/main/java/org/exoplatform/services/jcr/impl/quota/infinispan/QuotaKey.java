/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota.infinispan;

import org.exoplatform.services.jcr.infinispan.CacheKey;

/**
 * Common class is used to mark all keys related to quota cache.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: QuotaKey.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class QuotaKey extends CacheKey
{

   /**
    * QuotaKey constructor.
    */
   public QuotaKey()
   {
      super();
   }

   /**
    * QuotaKey constructor.
    */
   public QuotaKey(String ownerId, String id)
   {
      super(ownerId, id);
   }

   /**
    * QuotaKey constructor.
    */
   public QuotaKey(String ownerId, String id, String group)
   {
      super(ownerId, id, group);
   }
}
