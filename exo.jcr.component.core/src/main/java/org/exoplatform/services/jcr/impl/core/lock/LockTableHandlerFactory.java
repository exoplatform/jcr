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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.CacheableLockManagerImpl;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.JBCLockTableHandler;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.JBCShareableLockTableHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javax.sql.DataSource;

/**
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: LockTableHandlerFactory.java 34360 27.02.2012 11:55:03 dkuleshov $
 */
public class LockTableHandlerFactory
{
   protected static final Log LOG = ExoLogger.getExoLogger("exo.jcr.component.core.LockTableHandlerFactory");

   /**
    * Provides {@link LockTableHandler} instance according to preconfigured {@link WorkspaceLockManager}
    * 
    * @param workspaceEntry
    * @param lockManager
    * @return {@link LockTableHandler}
    */
   public static LockTableHandler getHandler(WorkspaceEntry workspaceEntry, AbstractCacheableLockManager lockManager)
   {
      String lockManagerFqn = workspaceEntry.getLockManager().getType();
      DataSource ds = getDataSource(lockManager);

      if ("org.exoplatform.services.jcr.impl.core.lock.jbosscache.CacheableLockManagerImpl".equals(lockManagerFqn))
      {
         if (isJbcCacheShareable(workspaceEntry))
         {
            return new JBCShareableLockTableHandler(workspaceEntry, ds);
         }
         return new JBCLockTableHandler(workspaceEntry, ds);
      }
      else if ("org.exoplatform.services.jcr.impl.core.lock.infinispan.ISPNCacheableLockManagerImpl"
         .equals(lockManagerFqn))
      {
         // we're using reflection to create IspnLockTableHandler instance
         // such aproach allows to avoid addition of jcr.component.core.infinispan.v5 as a dependency
         // (ispnLockTableHandler is located in jcr.component.core.infinispan.v5)
         // for jcr.component.core module, while it is needed only for compilation
         // and at the same time we can use IspnLockTableHandler for
         // jcr.component.core.infinispan.v5 module
         try
         {
            Class<?> ispnLockTableHandlerClass =
               Class.forName("org.exoplatform.services.jcr.impl.core.lock.infinispan.ISPNLockTableHandler");
            Constructor<?> ispnLockTableHandlerConstructor =
               ispnLockTableHandlerClass.getConstructor(WorkspaceEntry.class, DataSource.class);

            return (LockTableHandler)ispnLockTableHandlerConstructor.newInstance(workspaceEntry, ds);
         }
         catch (Throwable e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
      }
      else
      {
         throw new UnsupportedOperationException(
            "Currently supported only CacheableLockManagerImpl and ISPNCacheableLockManagerImpl");
      }
   }

   /**
    * Get {@link DataSource} value from field in {@link AbstractCacheableLockManager}.
    */
   private static DataSource getDataSource(AbstractCacheableLockManager lockManager)
   {
      DataSource ds;
      try
      {
         Field field = lockManager.getClass().getSuperclass().getDeclaredField("dataSource");
         field.setAccessible(true);

         ds = (DataSource)field.get(lockManager);
      }
      catch (Throwable e)
      {
         throw new IllegalStateException("Can't get access to the feild 'dataSource' of class "
            + lockManager.getClass());
      }

      if (ds == null)
      {
         throw new IllegalStateException("Datasource is null in class " + lockManager.getClass());
      }

      return ds;
   }

   private static Boolean isJbcCacheShareable(WorkspaceEntry workspaceEntry)
   {
      return workspaceEntry.getLockManager().getParameterBoolean(CacheableLockManagerImpl.JBOSSCACHE_SHAREABLE,
         CacheableLockManagerImpl.JBOSSCACHE_SHAREABLE_DEFAULT);
   }
}