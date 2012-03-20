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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sorting cache modification
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: CompressedISPNChangesBuffer.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public class CompressedISPNChangesBuffer
{
   private int historyIndex = 0;

   List<ChangesContainer> changes = new ArrayList<ChangesContainer>();

   /**
    * Adds new modification container to buffer and performs optimization if needed. Optimization doesn't iterate
    * over lists and uses HashMaps. So each optimization duration doesn't depend on list size.  
    * 
    * @param container
    */
   public void add(ChangesContainer container)
   {
      changes.add(container);
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

}
