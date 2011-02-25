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
package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: AbstractInputCacheStore.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public abstract class AbstractInputCacheStore extends AbstractCacheStore
{

   /**
    * 
    */
   public AbstractInputCacheStore()
   {
      super();
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#getConfigurationClass()
    */
   public Class<? extends CacheLoaderConfig> getConfigurationClass()
   {
      return AbstractCacheStoreConfig.class;
   }

   /**
    * @see org.infinispan.loaders.CacheStore#fromStream(java.io.ObjectInput)
    */
   public void fromStream(ObjectInput inputStream) throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.CacheStore#toStream(java.io.ObjectOutput)
    */
   public void toStream(ObjectOutput outputStream) throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.AbstractCacheStore#purgeInternal()
    */
   @Override
   protected void purgeInternal() throws CacheLoaderException
   {
      // This cacheStore only accepts data
   }

   /**
    * @see org.infinispan.loaders.CacheStore#clear()
    */
   public void clear() throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.CacheStore#remove(java.lang.Object)
    */
   public boolean remove(Object key) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return false;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#load(java.lang.Object)
    */
   public InternalCacheEntry load(Object key) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#load(int)
    */
   public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#loadAll()
    */
   public Set<InternalCacheEntry> loadAll() throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#loadAllKeys(java.util.Set)
    */
   public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

}