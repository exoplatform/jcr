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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.ChangesType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class ChangesContainerFactory
{
   protected static final Log LOG =
      ExoLogger.getLogger("org.exoplatform." +
      		"services.jcr.impl.dataflow.persistent.jbosscache.ChangesContatinerFactory");
   
   /**
    * Container for changes
    */
   public static abstract class ChangesContainer implements Comparable<ChangesContainer>
   {
      protected final Fqn fqn;

      protected final ChangesType changesType;

      protected final Cache<Serializable, Object> cache;

      protected final int historicalIndex;

      protected final boolean localMode;

      public ChangesContainer(Fqn fqn, ChangesType changesType, Cache<Serializable, Object> cache, int historicalIndex,
         boolean localMode)
      {
         super();
         this.fqn = fqn;
         this.changesType = changesType;
         this.cache = cache;
         this.historicalIndex = historicalIndex;
         this.localMode = localMode;
      }

      /**
       * @return the fqn
       */
      public Fqn getFqn()
      {
         return fqn;
      }

      /**
       * @return the index of change in original sequence
       */
      public int getHistoricalIndex()
      {
         return historicalIndex;
      }

      /**
       * @return the changesType
       */
      public ChangesType getChangesType()
      {
         return changesType;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return fqn + " type=" + changesType + " historyIndex=" + historicalIndex;
      }

      public int compareTo(ChangesContainer o)
      {
         int result = fqn.compareTo(o.getFqn());
         return result == 0 ? historicalIndex - o.getHistoricalIndex() : result;
      }

      protected void setCacheLocalMode()
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
      }

      public abstract void apply();
   }

   /**
    * Put object container;
    */
   public static class PutObjectContainer extends ChangesContainer
   {
      protected final Map<? extends Serializable, ? extends Object> data;

      public PutObjectContainer(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT, cache, historicalIndex, local);

         this.data = data;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, data);
      }
   }

   /**
    * Put  container.
    */
   public static class PutKeyValueContainer extends ChangesContainer
   {
      protected final Serializable key;

      protected final Object value;

      public PutKeyValueContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, key, value);
      }

      public Serializable getKey()
      {
         return key;
      }

      public Object getValue()
      {
         return value;
      }
      
   }

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back.
    * If null found, then new Set created (ordinary cache does).
    */
   public static class AddToListContainer extends ChangesContainer
   {
      protected final Serializable key;

      protected final Object value;

      public AddToListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         // object found by FQN and key;
         Object existingObject = cache.get(getFqn(), key);
         Set<Object> newSet = new HashSet<Object>();
         // if set found of null, perform add
         if (existingObject instanceof Set || existingObject == null)
         {
            // set found
            if (existingObject instanceof Set)
            {
               newSet.addAll((Set<Object>)existingObject);
            }
            newSet.add(value);
            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
         else
         {
            LOG.error("Unexpected object found by FQN:" + getFqn() + " and key:" + key + ". Expected Set, but found:"
               + existingObject.getClass().getName());
         }
      }
   }

   /**
    * It tries to get set by given key. If it is set then removes value and puts new modified set back.
    */
   public static class RemoveFromListContainer extends ChangesContainer
   {
      protected final Serializable key;

      protected final Object value;

      public RemoveFromListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         // object found by FQN and key;
         setCacheLocalMode();
         Object existingObject = cache.get(getFqn(), key);
         // if found value is really set! add to it.
         if (existingObject instanceof Set)
         {
            Set<Object> newSet = new HashSet<Object>((Set<Object>)existingObject);
            newSet.remove(value);
            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
      }
   }

   /**
    * Remove container.
    */
   public static class RemoveKeyContainer extends ChangesContainer
   {
      private final Serializable key;

      public RemoveKeyContainer(Fqn fqn, Serializable key, Cache<Serializable, Object> cache, int historicalIndex,
         boolean local)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local);
         this.key = key;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.remove(fqn, key);
      }

   }

   /**
    * Remove container.
    */
   public static class RemoveNodeContainer extends ChangesContainer
   {

      public RemoveNodeContainer(Fqn fqn, Cache<Serializable, Object> cache, int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.REMOVE, cache, historicalIndex, local);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.removeNode(fqn);
      }
   }
   
   public ChangesContainer createPutObjectContainer(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
            Cache<Serializable, Object> cache, int historicalIndex, boolean local)
   {
     return new PutObjectContainer(fqn, data, cache, historicalIndex, local);  
   }
   
   public ChangesContainer createPutKeyValueContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
            int historicalIndex, boolean local)
   {
     return new PutKeyValueContainer(fqn, key, value, cache, historicalIndex, local);  
   }
   
   public ChangesContainer createAddToListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
            int historicalIndex, boolean local)
   {
     return new AddToListContainer(fqn, key, value, cache, historicalIndex, local);
   }
   
   public ChangesContainer createRemoveFromListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
            int historicalIndex, boolean local)
   {
     return new RemoveFromListContainer(fqn, key, value, cache, historicalIndex, local);
   }
   
   public ChangesContainer createRemoveKeyContainer(Fqn fqn, Serializable key, Cache<Serializable, Object> cache, int historicalIndex,
            boolean local)
   {
     return new RemoveKeyContainer(fqn, key, cache, historicalIndex, local);
   }
   
   public ChangesContainer createRemoveNodeContainer(Fqn fqn, Cache<Serializable, Object> cache, int historicalIndex, boolean local)
   {
     return new RemoveNodeContainer(fqn, cache, historicalIndex, local);
   }
   
}
