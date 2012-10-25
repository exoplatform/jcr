/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.quota.BaseQuotaManager.ExceededQuotaBehavior;
import org.exoplatform.services.rpc.RPCService;

import java.util.concurrent.ExecutorService;

/**
 * Locates all entities in one place.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: WorkspaceQuotaContext.java 34360 Aug 23, 2012 tolusha $
 */
public class WorkspaceQuotaContext
{

   /**
    * Workspace name.
    */
   public final String wsName;

   /**
    * Repository name.
    */
   public final String rName;

   /**
    * Unique name.
    */
   public final String uniqueName;

   /**
    * {@link WorkspacePersistentDataManager}.
    */
   public final WorkspacePersistentDataManager dataManager;

   /**
    * {@link LocationFactory} instance.
    */
   public final LocationFactory lFactory;

   /**
    * Executor service per workspace to be able to suspend all tasks devoted current workspace..
    */
   public final ExecutorService executor;

   /**
    * {@link QuotaPersister}
    */
   public final QuotaPersister quotaPersister;

   /**
    * {@link RPCService}
    */
   public final RPCService rpcService;

   /**
    * @see ExceededQuotaBehavior
    */
   public final ExceededQuotaBehavior exceededQuotaBehavior;

   /**
    * WorkspaceQuotaContext constructor.
    */
   WorkspaceQuotaContext(String wsName, String rName, String uniqueName, WorkspacePersistentDataManager dataManager,
      LocationFactory lFactory, ExecutorService executor, QuotaPersister quotaPersister, RPCService rpcService,
      ExceededQuotaBehavior exceededQuotaBehavior)
   {
      this.wsName = wsName;
      this.rName = rName;
      this.uniqueName = uniqueName;
      this.dataManager = dataManager;
      this.lFactory = lFactory;
      this.executor = executor;
      this.quotaPersister = quotaPersister;
      this.rpcService = rpcService;
      this.exceededQuotaBehavior = exceededQuotaBehavior;
   }
}
