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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.loader.AdjListJDBCCacheLoaderConfig;

import java.sql.Connection;


/**
 * This class is used to override the method AdjListJDBCCacheLoader#tableExists in order
 * to more easily ensure multi-schema support.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class JDBCCacheLoader extends org.jboss.cache.loader.JDBCCacheLoader
{

   @Override
   protected boolean tableExists(String tableName, Connection con)
   {
      return JDBCUtils.tableExists(tableName, con);
   }
   
   /**
    * @see org.jboss.cache.loader.AdjListJDBCCacheLoader#processConfig(org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig)
    */
   @Override
   protected AdjListJDBCCacheLoaderConfig processConfig(CacheLoaderConfig.IndividualCacheLoaderConfig base)
   {
      AdjListJDBCCacheLoaderConfig config = super.processConfig(base);
      config.setClassName(getClass().getName());
      return config;
   }
}
