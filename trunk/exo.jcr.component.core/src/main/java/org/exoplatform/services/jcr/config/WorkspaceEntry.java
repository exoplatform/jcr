/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.config;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public class WorkspaceEntry
{

   protected String name;
   
   protected int lazyReadThreshold;

   @Deprecated
   protected String autoInitializedRootNt;

   protected ContainerEntry container;

   protected QueryHandlerEntry queryHandler;

   protected CacheEntry cache;

   protected transient String uniqueName;

   protected AccessManagerEntry accessManager;

   protected LockManagerEntry lockManager;

   protected WorkspaceInitializerEntry initializer;

   @Deprecated
   protected String autoInitPermissions;

   public WorkspaceEntry()
   {
   }

   @Deprecated
   public WorkspaceEntry(String name, String rootNt)
   {
      this.name = name;
      this.autoInitializedRootNt = rootNt;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return Returns the autoInitializedRootNt.
    */
   @Deprecated
   public String getAutoInitializedRootNt()
   {
      return autoInitializedRootNt;
   }

   /**
    * @param autoInitializedRootNt
    *          The autoInitializedRootNt to set.
    */
   @Deprecated
   public void setAutoInitializedRootNt(String autoInitializedRootNt)
   {
      this.autoInitializedRootNt = autoInitializedRootNt;
   }

   /**
    * @return Returns the container.
    */
   public ContainerEntry getContainer()
   {
      return container;
   }

   /**
    * @param container
    *          The container to set.
    */
   public void setContainer(ContainerEntry container)
   {
      this.container = container;
   }

   /**
    * @return Returns the cache.
    */
   public CacheEntry getCache()
   {
      return cache;
   }

   /**
    * @param cache
    *          The cache to set.
    */
   public void setCache(CacheEntry cache)
   {
      this.cache = cache;
   }

   /**
    * @return Returns the uniqueName.
    */
   public String getUniqueName()
   {
      return uniqueName;
   }

   /**
    * @param uniqueName
    *          The uniqueName to set.
    */
   public void setUniqueName(String uniqueName)
   {
      this.uniqueName = uniqueName;
   }

   public AccessManagerEntry getAccessManager()
   {
      return accessManager;
   }

   public void setAccessManager(AccessManagerEntry accessManager)
   {
      this.accessManager = accessManager;
   }

   @Deprecated
   public String getAutoInitPermissions()
   {
      return autoInitPermissions;
   }

   @Deprecated
   public void setAutoInitPermissions(String autoInitPermissions)
   {
      this.autoInitPermissions = autoInitPermissions;
   }

   public LockManagerEntry getLockManager()
   {
      return lockManager;
   }

   public void setLockManager(LockManagerEntry lockManager)
   {
      this.lockManager = lockManager;
   }

   public QueryHandlerEntry getQueryHandler()
   {
      return queryHandler;
   }

   public void setQueryHandler(QueryHandlerEntry queryHandlerEntry)
   {
      this.queryHandler = queryHandlerEntry;
   }

   public WorkspaceInitializerEntry getInitializer()
   {
      return initializer;
   }

   public void setInitializer(WorkspaceInitializerEntry initializer)
   {
      this.initializer = initializer;
   }
   
   public int getLazyReadThreshold()
   {
      return lazyReadThreshold;
   }

   public void setLazyReadThreshold(int lazyReadThreshold)
   {
      this.lazyReadThreshold = lazyReadThreshold;
   }   

}
