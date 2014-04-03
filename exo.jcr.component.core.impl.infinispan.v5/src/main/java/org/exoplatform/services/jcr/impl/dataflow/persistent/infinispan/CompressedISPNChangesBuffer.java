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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.BufferedISPNCache.ChangesContainer;
import org.exoplatform.services.jcr.infinispan.CacheKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sorting cache modification
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: CompressedISPNChangesBuffer.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public class CompressedISPNChangesBuffer
{
   private int historyIndex = 0;

   private final List<ChangesContainer> changes = new ArrayList<ChangesContainer>();

   /**
    * Stores the last changes into the map by CacheKey to be able to find a change quickly 
    */
   private final Map<CacheKey, Object> lastChanges = new HashMap<CacheKey, Object>();

   /**
    * Indicates whether or not the invalidation is enabled
    */
   private final boolean invalidationEnabled;

   public CompressedISPNChangesBuffer()
   {
      this(false);
   }

   public CompressedISPNChangesBuffer(boolean invalidationEnabled)
   {
      this.invalidationEnabled = invalidationEnabled;
   }

   /**
    * Indicates whether or not the invalidation is enabled
    */
   public boolean isInvalidationEnabled()
   {
      return invalidationEnabled;
   }

   /**
    * Adds new modification container to buffer and performs optimization if needed. Optimization doesn't iterate
    * over lists and uses HashMaps. So each optimization duration doesn't depend on list size.  
    * 
    * @param container
    */
   public void add(ChangesContainer container)
   {
      changes.add(container);
      container.setInvalidation(invalidationEnabled);
      container.applyToBuffer(this);
   }

   /**
    * After each invocation of the method increments internal field. 
    * Designed to be used as history order index in each {@link ChangesContainer}
    * @return
    */
   public int getHistoryIndex()
   {
      historyIndex++;
      return historyIndex;
   }

   /**
    * Builds single list of modifications from internal structures and sorts it.
    * 
    * @return
    */
   public List<ChangesContainer> getSortedList()
   {
      List<ChangesContainer> changesContainers = new ArrayList<ChangesContainer>(changes);
      Collections.sort(changesContainers);
      return changesContainers;
   }

   Object get(CacheKey key)
   {
      return lastChanges.get(key);
   }

   void put(CacheKey key, Object value)
   {
      lastChanges.put(key, value);
   }

   /**
    * @return that latest changes applied into the buffer
    */
   public Map<CacheKey, Object> getLastChanges()
   {
      return lastChanges;
   }
}
