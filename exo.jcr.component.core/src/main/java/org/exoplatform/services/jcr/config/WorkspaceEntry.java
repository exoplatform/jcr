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
 * @LevelAPI Unsupported
 */

public class WorkspaceEntry
{

   protected String name;
   
   protected int lazyReadThreshold;

   protected ContainerEntry container;

   protected QueryHandlerEntry queryHandler;

   protected CacheEntry cache;

   protected transient String uniqueName;

   protected AccessManagerEntry accessManager;

   protected LockManagerEntry lockManager;

   protected WorkspaceInitializerEntry initializer;

   public WorkspaceEntry()
   {
   }
   /**
    * @return returns the workspace name
    */
   public String getName()
   {
      return name;
   }
   /**
    * @param name the workspace name
    */
   public void setName(String name)
   {
      this.name = name;
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
   /**
    * @return returns the Access Manager
    */
   public AccessManagerEntry getAccessManager()
   {
      return accessManager;
   }
   /**
    * @param accessManager the Access Manager
    */
   public void setAccessManager(AccessManagerEntry accessManager)
   {
      this.accessManager = accessManager;
   }
   /**
    * @return returns the Lock Manager
    */
   public LockManagerEntry getLockManager()
   {
      return lockManager;
   }
   /**
    * @param lockManager the Lock Manager
    */
   public void setLockManager(LockManagerEntry lockManager)
   {
      this.lockManager = lockManager;
   }
   /**
    * @return returns the Query Handler
    */
   public QueryHandlerEntry getQueryHandler()
   {
      return queryHandler;
   }
   /**
    * @param queryHandlerEntry the Query Handler
    */
   public void setQueryHandler(QueryHandlerEntry queryHandlerEntry)
   {
      this.queryHandler = queryHandlerEntry;
   }
   /**
    * @return returns the Workspace Initializer
    */
   public WorkspaceInitializerEntry getInitializer()
   {
      return initializer;
   }
   /**
    * @param initializer the workspace initializer
    */
   public void setInitializer(WorkspaceInitializerEntry initializer)
   {
      this.initializer = initializer;
   }
   /**
    * @return returns the Load Threshold
    */
   public int getLazyReadThreshold()
   {
      return lazyReadThreshold;
   }
   /**
    * @param lazyReadThreshold the Load Threshold
    */
   public void setLazyReadThreshold(int lazyReadThreshold)
   {
      this.lazyReadThreshold = lazyReadThreshold;
   }   

}
