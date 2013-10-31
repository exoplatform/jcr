/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.transaction.xa.GlobalTransaction;

import java.util.List;

/**
 * This JDBC Cache store is needed to be able to ensure consistency between the cache and the database.
 *
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
@CacheLoaderMetadata(configurationClass = JdbcBinaryCacheStoreConfig.class)
public class JdbcBinaryCacheStore extends org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStore
{
   /**
    * @see org.infinispan.loaders.jdbc.stringbased.JdbcBinaryCacheStore#getConfigurationClass()
    */
   @Override
   public Class<? extends CacheLoaderConfig> getConfigurationClass()
   {
      return JdbcBinaryCacheStoreConfig.class;
   }

   private ManagedConnectionFactory getManagedConnectionFactory()
   {
      return (ManagedConnectionFactory)getConnectionFactory();
   }

   /**
    * @see org.infinispan.loaders.AbstractCacheStore#prepare(java.util.List, org.infinispan.transaction.xa.GlobalTransaction, boolean)
    */
   @Override
   public void prepare(List<? extends Modification> mods, GlobalTransaction tx, boolean isOnePhase)
      throws CacheLoaderException
   {
      // start a tx
      getManagedConnectionFactory().prepare(tx);
      // Put modifications
      super.prepare(mods, tx, true);
      // commit if it's one phase only
      if (isOnePhase) commit(tx);
   }

   /**
    * @see org.infinispan.loaders.AbstractCacheStore#rollback(org.infinispan.transaction.xa.GlobalTransaction)
    */
   @Override
   public void rollback(GlobalTransaction tx)
   {
      super.rollback(tx);
      getManagedConnectionFactory().rollback(tx);
   }

   /**
    * @see org.infinispan.loaders.AbstractCacheStore#commit(org.infinispan.transaction.xa.GlobalTransaction)
    */
   @Override
   public void commit(GlobalTransaction tx) throws CacheLoaderException
   {
      super.commit(tx);
      getManagedConnectionFactory().commit(tx);
   }
}
