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

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS.
 * 
 *  ChahgesContainerExpirationFactory this special factory to ExpirationAlgorithm
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id$
 */
public class ChangesContainerExpirationFactory
extends ChangesContainerFactory
{

   /**
    * The expiration timeout.
    */
   public static long expirationTimeOut;

   public ChangesContainerExpirationFactory(long expirationTimeOut)
   {
      this.expirationTimeOut = expirationTimeOut;
   }

   /**
    * Put object container;
    */
   public static class PutObjectContainerExpiration extends PutObjectContainer
   {

      public PutObjectContainerExpiration(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local)
      {
         super(fqn, data, cache, historicalIndex, local);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, data);

         setCacheLocalMode();
         cache.put(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + expirationTimeOut));
      }
   }

   /**
    * Put  container.
    */
   public static class PutKeyValueContainerExpiration extends PutKeyValueContainer
   {

      public PutKeyValueContainerExpiration(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, key, value, cache, historicalIndex, local);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + expirationTimeOut));

         setCacheLocalMode();
         cache.put(fqn, key, value);
      }

      @Override
      public Serializable getKey()
      {
         return key;
      }

      @Override
      public Object getValue()
      {
         return value;
      }

   }

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back.
    * If null found, then new Set created (ordinary cache does).
    */
   public static class AddToListContainerExpiration extends AddToListContainer
   {

      public AddToListContainerExpiration(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, key, value, cache, historicalIndex, local);
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
            cache.put(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + expirationTimeOut));

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
   public static class RemoveFromListContainerExpiration extends RemoveFromListContainer
   {

      public RemoveFromListContainerExpiration(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, key, value, cache, historicalIndex, local);
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
            cache.put(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + expirationTimeOut));

            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
      }
   }

   @Override
   public ChangesContainer createPutObjectContainer(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
      Cache<Serializable, Object> cache, int historicalIndex, boolean local)
   {
      return new PutObjectContainerExpiration(fqn, data, cache, historicalIndex, local);
   }

   @Override
   public ChangesContainer createPutKeyValueContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
      int historicalIndex, boolean local)
   {
      return new PutKeyValueContainerExpiration(fqn, key, value, cache, historicalIndex, local);
   }

   @Override
   public ChangesContainer createAddToListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
      int historicalIndex, boolean local)
   {
      return new AddToListContainerExpiration(fqn, key, value, cache, historicalIndex, local);
   }

   @Override
   public ChangesContainer createRemoveFromListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
      int historicalIndex, boolean local)
   {
      return new RemoveFromListContainerExpiration(fqn, key, value, cache, historicalIndex, local);
   }

}
