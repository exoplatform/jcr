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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.bucket.Bucket;
import org.infinispan.loaders.jdbc.JdbcUtil;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.transaction.xa.GlobalTransaction;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JdbcBinaryCacheStore");

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

   @Override
   protected Bucket loadBucket(Integer keyHashCode) throws CacheLoaderException {
      Connection conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
         String sql = getTableManipulation().getSelectRowSql();
         if (LOG.isTraceEnabled()) {
            LOG.trace("Running loadBucket. Sql: '{}', on key: {}", sql, keyHashCode);
         }
         conn = getConnectionFactory().getConnection();
         ps = conn.prepareStatement(sql);
         ps.setString(1, keyHashCode.toString());
         rs = ps.executeQuery();
         if (!rs.next()) {
            return null;
         }
         String bucketName = rs.getString(1);
         InputStream inputStream = rs.getBinaryStream(2);
         Bucket bucket = (Bucket) JdbcUtil.unmarshall(getMarshaller(), inputStream);
         bucket.setBucketId(bucketName);//bucket name is volatile, so not persisted.
         return bucket;
      } catch (SQLException e) {
         LOG.error("Sql failure while loading key: {}", String.valueOf(keyHashCode));
         throw new CacheLoaderException(String.format(
               "Sql failure while loading key: %s", keyHashCode), e);
      } finally {
         JdbcUtil.safeClose(rs);
         JdbcUtil.safeClose(ps);
         getConnectionFactory().releaseConnection(conn);
      }
   }
}
