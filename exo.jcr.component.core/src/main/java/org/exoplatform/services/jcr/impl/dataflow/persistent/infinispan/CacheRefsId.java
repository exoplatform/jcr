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

package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.services.jcr.infinispan.CacheKey;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 10.06.2008
 * 
 * Cache record used to store item Id key.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CachePropsId.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public class CacheRefsId extends CacheKey
{
   public CacheRefsId()
   {
      super();
   }

   CacheRefsId(String ownerId, String id)
   {
      super(ownerId, id);
   }
}
