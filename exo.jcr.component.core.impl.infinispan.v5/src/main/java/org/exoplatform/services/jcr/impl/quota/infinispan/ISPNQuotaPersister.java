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
package org.exoplatform.services.jcr.impl.quota.infinispan;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.quota.AbstractQuotaPersister;
import org.exoplatform.services.jcr.impl.quota.UnknownDataSizeException;
import org.exoplatform.services.jcr.impl.quota.UnknownQuotaLimitException;
import org.exoplatform.services.jcr.infinispan.CacheKey;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ISPNQuotaPersister.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ISPNQuotaPersister extends AbstractQuotaPersister
{

   /**
    * ISPN cache.
    */
   private AdvancedCache<Serializable, Object> cache;

   /**
    * ISPNQuotaPersister constructor.
    */
   protected ISPNQuotaPersister(MappedParametrizedObjectEntry entry, ConfigurationManager cfm)
      throws RepositoryConfigurationException
   {
      ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm);

      cache = factory.createCache("quota", entry).getAdvancedCache();
      cache.start();
   }

   /**
    * {@inheritDoc}
    */
   public void destroy()
   {
      cache.stop();
      ISPNCacheFactory.releaseUniqueInstance(cache.getCacheManager());
   }

   /**
    * {@inheritDoc}
    */
   public void setNodeQuota(String repositoryName, String workspaceName, String nodePath, long quotaLimit,
      boolean asyncUpdate)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);

      CacheKey key = new NodeQuotaKey(workspaceUniqueName, nodePath);
      QuotaValue value = new QuotaValue(quotaLimit, asyncUpdate);

      cache.put(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public void setGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath, long quotaLimit,
      boolean asyncUpdate)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);

      CacheKey key = new GroupOfNodesQuotaKey(workspaceUniqueName, patternPath);
      QuotaValue value = new QuotaValue(quotaLimit, asyncUpdate);

      cache.put(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public long getNodeDataSize(String repositoryName, String workspaceName, String nodePath)
      throws UnknownDataSizeException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeDataSizeKey(workspaceUniqueName, nodePath);

      return getDataSize(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setNodeDataSize(String repositoryName, String workspaceName, String nodePath, long dataSize)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeDataSizeKey(workspaceUniqueName, nodePath);

      cache.put(key, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeDataSize(String repositoryName, String workspaceName, String nodePath)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeDataSizeKey(workspaceUniqueName, nodePath);

      cache.remove(key);
   }

   /**
    * {@inheritDoc}
    */
   public long getWorkspaceQuota(String repositoryName, String workspaceName) throws UnknownQuotaLimitException
   {
      String wsUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new WorkspaceQuotaKey(wsUniqueName);

      return getQuota(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setWorkspaceQuota(String repositoryName, String workspaceName, long quotaLimit)
   {
      String wsUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new WorkspaceQuotaKey(wsUniqueName);

      cache.put(key, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeWorkspaceQuota(String repositoryName, String workspaceName)
   {
      String wsUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new WorkspaceQuotaKey(wsUniqueName);

      cache.remove(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setWorkspaceDataSize(String repositoryName, String workspaceName, long dataSize)
   {
      String wsUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new WorkspaceDataSizeKey(wsUniqueName);

      cache.put(key, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public long getWorkspaceDataSize(String repositoryName, String workspaceName) throws UnknownDataSizeException
   {
      String wsUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new WorkspaceDataSizeKey(wsUniqueName);

      return getDataSize(key);
   }

   /**
    * {@inheritDoc}
    */
   public long getRepositoryQuota(String repositoryName) throws UnknownQuotaLimitException
   {
      CacheKey key = new RepositoryQuotaKey(repositoryName);
      return getQuota(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setRepositoryQuota(String repositoryName, long quotaLimit)
   {
      CacheKey key = new RepositoryQuotaKey(repositoryName);
      cache.put(key, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepositoryQuota(String repositoryName)
   {
      CacheKey key = new RepositoryQuotaKey(repositoryName);
      cache.remove(key);
   }

   /**
    * {@inheritDoc}
    */
   public long getRepositoryDataSize(String repositoryName) throws UnknownDataSizeException
   {
      CacheKey key = new RepositoryDataSizeKey(repositoryName);
      return getDataSize(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setRepositoryDataSize(String repositoryName, long dataSize)
   {
      CacheKey key = new RepositoryDataSizeKey(repositoryName);
      cache.put(key, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public long getGlobalDataSize() throws UnknownDataSizeException
   {
      CacheKey key = new GlobalDataSizeKey();
      return getDataSize(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setGlobalDataSize(long dataSize)
   {
      CacheKey key = new GlobalDataSizeKey();
      cache.put(key, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public long getGlobalQuota() throws UnknownQuotaLimitException
   {
      CacheKey key = new GlobalQuotaKey();
      return getQuota(key);
   }

   /**
    * {@inheritDoc}
    */
   public void setGlobalQuota(long quotaLimit)
   {
      CacheKey key = new GlobalQuotaKey();
      cache.put(key, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeGlobalQuota()
   {
      CacheKey key = new GlobalQuotaKey();
      cache.remove(key);
   }

   /**
    * {@inheritDoc}
    */
   public void clearWorkspaceData(String repositoryName, String workspaceName) throws BackupException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);

      for (Serializable cacheKey : cache.keySet())
      {
         if (cacheKey instanceof WorkspaceBasedKey)
         {
            if (workspaceUniqueName.equals(((WorkspaceBasedKey)cacheKey).getWorkspaceUniqueName()))
            {
               cache.remove(cacheKey);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getNodeQuota(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeQuotaKey(workspaceUniqueName, nodePath);

      return getQuotaValue(key).getQuotaLimit();
   }

   /**
    * {@inheritDoc}
    */
   public long getGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath)
      throws UnknownQuotaLimitException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new GroupOfNodesQuotaKey(workspaceUniqueName, patternPath);

      return getQuotaValue(key).getQuotaLimit();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeQuotaAsync(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeQuotaKey(workspaceUniqueName, nodePath);

      return getQuotaValue(key).getAsyncUpdate();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isGroupOfNodesQuotaAsync(String repositoryName, String workspaceName, String patternPath)
      throws UnknownQuotaLimitException
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new GroupOfNodesQuotaKey(workspaceUniqueName, patternPath);

      return getQuotaValue(key).getAsyncUpdate();
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllNodeQuota(String repositoryName, String workspaceName)
   {
      Set<String> pathes = new HashSet<String>();

      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      for (Serializable cacheKey : cache.keySet())
      {
         if (cacheKey instanceof NodeQuotaKey)
         {
            if (workspaceUniqueName.equals(((WorkspaceBasedKey)cacheKey).getWorkspaceUniqueName()))
            {
               pathes.add(((PathBasedKey)cacheKey).getPath());
            }
         }
      }

      return pathes;
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllGroupOfNodesQuota(String repositoryName, String workspaceName)
   {
      Set<String> pathes = new HashSet<String>();

      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      for (Serializable cacheKey : cache.keySet())
      {
         if (cacheKey instanceof GroupOfNodesQuotaKey)
         {
            if (workspaceUniqueName.equals(((WorkspaceBasedKey)cacheKey).getWorkspaceUniqueName()))
            {
               pathes.add(((PathBasedKey)cacheKey).getPath());
            }
         }
      }

      return pathes;
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllTrackedNodes(String repositoryName, String workspaceName)
   {
      Set<String> pathes = new HashSet<String>();

      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      for (Serializable cacheKey : cache.keySet())
      {
         if (cacheKey instanceof NodeDataSizeKey)
         {
            if (workspaceUniqueName.equals(((WorkspaceBasedKey)cacheKey).getWorkspaceUniqueName()))
            {
               pathes.add(((PathBasedKey)cacheKey).getPath());
            }
         }
      }

      return pathes;
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeQuota(String repositoryName, String workspaceName, String nodePath)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new NodeQuotaKey(workspaceUniqueName, nodePath);

      cache.remove(key);
   }

   /**
    * {@inheritDoc}
    */
   public void removeGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath)
   {
      String workspaceUniqueName = composeWorkspaceUniqueName(repositoryName, workspaceName);
      CacheKey key = new GroupOfNodesQuotaKey(workspaceUniqueName, patternPath);

      cache.remove(key);
   }

   // ==============================> private methods

   /**
    * Returns quota limit value otherwise throws {@link UnknownQuotaLimitException}.
    */
   private long getQuota(CacheKey key) throws UnknownQuotaLimitException
   {
      Long size = (Long)cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

      if (size == null)
      {
         throw new UnknownQuotaLimitException("Quota was not set early");
      }

      return size;
   }

   /**
    * Returns {@link QuotaValue} otherwise throws {@link UnknownQuotaLimitException}.
    */
   private QuotaValue getQuotaValue(CacheKey key) throws UnknownQuotaLimitException
   {
      QuotaValue quotaValue = (QuotaValue)cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

      if (quotaValue == null)
      {
         throw new UnknownQuotaLimitException("Quota was not set early");
      }

      return quotaValue;
   }

   /**
    * Returns data size value otherwise throws {@link UnknownDataSizeException}.
    */
   private long getDataSize(CacheKey key) throws UnknownDataSizeException
   {
      Long size = (Long)cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

      if (size == null)
      {
         throw new UnknownDataSizeException("Data size is unknown");
      }

      return size;
   }

   /**
    * Compose unique workspace name in global JCR instance.
    */
   private String composeWorkspaceUniqueName(String repositoryName, String workspaceName)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(repositoryName);
      builder.append('/');
      builder.append(workspaceName);
      builder.append('/');

      return builder.toString();
   }

}
