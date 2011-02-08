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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.eviction.EvictionActionPolicy;

/**
 * This class is used to prevent the memory leak described here http://community.jboss.org/thread/147084
 * and corresponding to the JIRA https://jira.jboss.org/jira/browse/JBCACHE-1567
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 22 janv. 2010  
 */
public class ParentNodeEvictionActionPolicy implements EvictionActionPolicy
{
   Cache<?, ?> cache;

   private static final Log log = LogFactory.getLog("exo.jcr.component.core.DefaultEvictionActionPolicy");

   public void setCache(Cache<?, ?> cache)
   {
      this.cache = cache;
   }

   @SuppressWarnings("unchecked")
   public boolean evict(Fqn fqn)
   {
      boolean result;
      try
      {
         if (log.isTraceEnabled())
         {
            log.trace("Evicting Fqn " + fqn);
         }
         cache.evict(fqn);
         result = true;
      }
      catch (Exception e)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Unable to evict " + fqn, e);
         }
         result = false;
      }
      if (fqn.size() != 4)
      {
         return result;
      }
      try
      {
         Fqn parentFqn = fqn.getParent();
         if (parentFqn.get(1).equals(JBossCacheWorkspaceStorageCache.CHILD_NODES)
            || parentFqn.get(1).equals(JBossCacheWorkspaceStorageCache.CHILD_PROPS))
         {
            // The expected data structure is of type ${ws-id}/$CHILD_NODES/${node-id}/${sub-node-name} or
            // ${ws-id}/$CHILD_PROPS/${node-id}/${sub-property-name}

            // We use the method hasChildrenDirect to avoid going through 
            // the intercepter chain (EXOJCR-460)
            NodeSPI node = ((CacheSPI)cache).peek(parentFqn, false);
            // Check if not null, possibly this node was concurrently removed 
            if (node != null && !node.hasChildrenDirect())
            {
               if (log.isTraceEnabled())
               {
                  log.trace("Evicting Fqn " + fqn);
               }
               cache.evict(parentFqn);
            }
         }
      }
      catch (Exception e)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Unable to evict " + fqn, e);
         }
      }
      return result;
   }
}
