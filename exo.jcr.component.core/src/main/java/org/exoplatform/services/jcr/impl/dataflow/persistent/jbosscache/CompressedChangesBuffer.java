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
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.ChangesContainer;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.ChangesType;
import org.jboss.cache.Fqn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sorting cache modification "as is" in {@link BufferedJBossCache} may harm data consistency. 
 * Here is a link, showing possible trouble:
 * <a  href="http://wiki-int.exoplatform.org/display/exoproducts/Problems+encountered+2010.01.08">Wiki-int page 
 * "Why straightforward sorting in cache is not working..." </a>
 * <br>
 * <strong>Example on the page refers to the old cache-usage structure, where list of 
 * child items was stored as JBC nodes. </strong> <br>
 *    For now serialized Set<Object> is used, and it is stored as a JCB attribute. But still 
 * such confusing situation can take place in CHILD_NODES and CHILD_PROPS subtrees in cache.
 * So wee need to optimize changes made for one subtree with different Fqn levels.<br> 
 * I.e.
 * <ol> 
 *   <li type="1">ADD /$CHILD_NODES/1/11</li> 
 *   <li type="1">REMOVE /$CHILD_NODES/1</li>
 * </ol>
 * Sorting will reorder them in different way, and there is a possible situation when ADD will be after RM.
 * So list of changes should be optimized, before being applied. This class performs described modification
 * for only CHILD_NODES and CHILD_PROPS cache regions using HashMap for the best performance.<br>
 * <small>Class if not thread safe, cause designed to be used in ThreadLocal variable in {@link BufferedJBossCache}.</small>
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: CompressedChangesBuffer.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class CompressedChangesBuffer
{
   private int historyIndex = 0;

   /**
    *  Stores changes made to any ordinary cache region
    */
   private final List<ChangesContainer> changes = new ArrayList<ChangesContainer>();

   /**
    * Stores the last changes into the map by fqn to be able to find a change quickly 
    */
   private final Map<Fqn, Map<Serializable, Object>> lastChanges = new HashMap<Fqn, Map<Serializable, Object>>();

   /**
    *  Stores changes made to /$CHILD_NODES cache region. Stores <ParentUUID> <--> <Change>
    */
   private final Map<String, List<ChangesContainer>> childNodesMap = new HashMap<String, List<ChangesContainer>>();

   /**
    *  Stores changes made to /$CHILD_PROPERTIES cache region. Stores <ParentUUID> <--> <Change>
    */
   private final Map<String, List<ChangesContainer>> childPropertyMap = new HashMap<String, List<ChangesContainer>>();

   /**
    * Adds new modification container to buffer and performs optimization if needed. Optimization doesn't iterate
    * over lists and uses HashMaps. So each optimization duration doesn't depend on list size.  
    * 
    * @param container
    */
   public void add(ChangesContainer container)
   {
      String parentCacheNode = (String)container.getFqn().get(1);
      if (container.getFqn().size() > 2 && JBossCacheWorkspaceStorageCache.CHILD_NODES.equals(parentCacheNode))
      {
         optimize(childNodesMap, container);
      }
      else if (container.getFqn().size() > 2 && JBossCacheWorkspaceStorageCache.CHILD_PROPS.equals(parentCacheNode))
      {
         optimize(childPropertyMap, container);
      }
      else
      {
         changes.add(container);
      }
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
      List<ChangesContainer> changesContainers = listMap(childNodesMap);
      changesContainers.addAll(listMap(childPropertyMap));
      changesContainers.addAll(changes);
      Collections.sort(changesContainers);
      return changesContainers;
   }

   // non-public members

   /**
    * Adds given container to the list of parent from the Map. If list is missing in the map, it is created and placed.
    * 
    * @param childMap
    * @param parent
    * @param container
    */
   private void addToList(Map<String, List<ChangesContainer>> childMap, String parent, ChangesContainer container)
   {
      List<ChangesContainer> changesContainers = childMap.get(parent);
      if (changesContainers == null)
      {
         changesContainers = new ArrayList<ChangesContainer>();
      }
      changesContainers.add(container);
      childMap.put(parent, changesContainers);
   }

   /**
    * Builds single list from map containing a list by each key.
    * 
    * @param childMap
    * @return
    */
   private List<ChangesContainer> listMap(Map<String, List<ChangesContainer>> childMap)
   {
      List<ChangesContainer> containers = new ArrayList<ChangesContainer>();
      for (List<ChangesContainer> container : childMap.values())
      {
         containers.addAll(container);
      }
      return containers;
   }

   /**
    * Performs described optimization on buffer.
    * 
    * @param childMap
    * @param container
    */
   private void optimize(Map<String, List<ChangesContainer>> childMap, ChangesContainer container)
   {
      Fqn fqn = container.getFqn();
      String parent = (String)fqn.get(2);
      if (fqn.size() == 3)
      {
         if (container.getChangesType() == ChangesType.REMOVE)
         {
            // omit all changes made on exact fqn and child-fqns
            childMap.remove(parent);
         }
      }
      addToList(childMap, parent, container);
   }

   Object get(Fqn fqn, Serializable key)
   {
      Map<Serializable, Object> map = lastChanges.get(fqn);
      if (map != null)
      {
         return map.get(key);
      }
      return null;
   }

   void put(Fqn fqn, Serializable key, Object value)
   {
      Map<Serializable, Object> map = lastChanges.get(fqn);
      if (map == null)
      {
         map = new HashMap<Serializable, Object>();
         lastChanges.put(fqn, map);
      }
      map.put(key, value);
   }

   /**
    * @return that latest changes applied into the buffer
    */
   public Map<Fqn, Map<Serializable, Object>> getLastChanges()
   {
      return lastChanges;
   }
}
