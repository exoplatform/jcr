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
package org.exoplatform.services.jcr.impl.quota.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.quota.AbstractQuotaPersister;
import org.exoplatform.services.jcr.impl.quota.UnknownDataSizeException;
import org.exoplatform.services.jcr.impl.quota.UnknownQuotaLimitException;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Set;

/**
 * Cache structure:
 * <ul>
 *    <li>$QUOTA</li>
 *    <li>$DATA_SIZE</li>
 * </ul>
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: JBCQuotaPersister.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class JBCQuotaPersister extends AbstractQuotaPersister
{

   /**
    * JBoss cache.
    */
   protected Cache<Serializable, Object> cache;

   /**
    * Based region where allowed quota sized is stored. Should not be covered by eviction.
    */
   public static final Fqn<String> QUOTA = Fqn.fromElements("$QUOTA");

   /**
    * Based region where used quota sized is stored. May be covered by eviction.
    */
   public static final Fqn<String> DATA_SIZE = Fqn.fromElements("$DATA_SIZE");

   /**
    * Key name.
    */
   public static final String SIZE = "$SIZE";

   /**
    * Key name.
    */
   public static final String ASYNC_UPATE = "$ASYNC_UPATE";

   /**
    * Relative element name.
    */
   public static final String QUOTA_PATHS = "$PATHS";

   /**
    * Relative element name.
    */
   public static final String QUOTA_PATTERNS = "$PATTERNS";

   /**
    * JBCQuotaPersister constructor.
    */
   protected JBCQuotaPersister(MappedParametrizedObjectEntry entry, ConfigurationManager cfm)
      throws RepositoryConfigurationException
   {
      // create cache using custom factory
      ExoJBossCacheFactory<Serializable, Object> factory = new ExoJBossCacheFactory<Serializable, Object>(cfm);

      cache = factory.createCache(entry);
      cache = ExoJBossCacheFactory.getUniqueInstance(CacheType.QUOTA_CACHE, Fqn.ROOT, cache, true);
      cache.create();
      cache.start();

      createResidentNode(QUOTA);
      createResidentNode(DATA_SIZE);
   }

   /**
    * {@inheritDoc}
    */
   public void setNodeQuota(String repositoryName, String workspaceName, String nodePath, long quotaLimit,
      boolean asyncUpdate)
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATHS, nodePath);
      cache.put(fqn, SIZE, quotaLimit);
      cache.put(fqn, ASYNC_UPATE, asyncUpdate);
   }

   /**
    * {@inheritDoc}
    */
   public void setGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath, long quotaLimit,
      boolean asyncUpdate)
   {
      patternPath = escaping(patternPath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATTERNS, patternPath);
      cache.put(fqn, SIZE, quotaLimit);
      cache.put(fqn, ASYNC_UPATE, asyncUpdate);
   }

   /**
    * {@inheritDoc}
    */
   public long getNodeDataSize(String repositoryName, String workspaceName, String nodePath)
      throws UnknownDataSizeException
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName, nodePath);
      return getDataSize(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void setNodeDataSize(String repositoryName, String workspaceName, String nodePath, long dataSize)
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName, nodePath);
      cache.put(fqn, SIZE, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeDataSize(String repositoryName, String workspaceName, String nodePath)
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName, nodePath);
      cache.removeNode(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeQuota(String repositoryName, String workspaceName, String nodePath)
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATHS, nodePath);
      cache.removeNode(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void removeGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath)
   {
      patternPath = escaping(patternPath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATTERNS, patternPath);
      cache.removeNode(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public long getWorkspaceQuota(String repositoryName, String workspaceName) throws UnknownQuotaLimitException
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName);
      return getQuota(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void setWorkspaceQuota(String repositoryName, String workspaceName, long quotaLimit)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName);
      cache.put(fqn, SIZE, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeWorkspaceQuota(String repositoryName, String workspaceName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName);
      cache.remove(fqn, SIZE);
   }

   /**
    * {@inheritDoc}
    */
   public void setWorkspaceDataSize(String repositoryName, String workspaceName, long dataSize)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName);
      cache.put(fqn, SIZE, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public long getWorkspaceDataSize(String repositoryName, String workspaceName) throws UnknownDataSizeException
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName);
      return getDataSize(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public long getRepositoryDataSize(String repositoryName) throws UnknownDataSizeException
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName);
      return getDataSize(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void setRepositoryDataSize(String repositoryName, long dataSize)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName);
      cache.put(fqn, SIZE, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public long getRepositoryQuota(String repositoryName) throws UnknownQuotaLimitException
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName);
      return getQuota(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public void setRepositoryQuota(String repositoryName, long quotaLimit)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName);
      cache.put(fqn, SIZE, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepositoryQuota(String repositoryName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName);
      cache.remove(fqn, SIZE);
   }

   /**
    * {@inheritDoc}
    */
   public long getGlobalQuota() throws UnknownQuotaLimitException
   {
      return getQuota(QUOTA);
   }

   /**
    * {@inheritDoc}
    */
   public void setGlobalQuota(long quotaLimit)
   {
      cache.put(QUOTA, SIZE, quotaLimit);
   }

   /**
    * {@inheritDoc}
    */
   public void removeGlobalQuota()
   {
      cache.remove(QUOTA, SIZE);
   }

   /**
    * {@inheritDoc}
    */
   public long getGlobalDataSize() throws UnknownDataSizeException
   {
      return getDataSize(DATA_SIZE);
   }

   /**
    * @inheritDoc}
    */
   public void setGlobalDataSize(long dataSize)
   {
      cache.put(DATA_SIZE, SIZE, dataSize);
   }

   /**
    * {@inheritDoc}
    */
   public void destroy()
   {
      try
      {
         ExoJBossCacheFactory.releaseUniqueInstance(CacheType.QUOTA_CACHE, cache);
      }
      catch (RepositoryConfigurationException e)
      {
         LOG.error("Can not release cache instance", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clearWorkspaceData(String repositoryName, String workspaceName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(JBCQuotaPersister.DATA_SIZE, repositoryName, workspaceName);
      cache.removeNode(fqn);

      fqn = Fqn.fromRelativeElements(JBCQuotaPersister.QUOTA, repositoryName, workspaceName);
      cache.removeNode(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeQuotaAsync(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATHS, nodePath);
      return getAsyncUpdate(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isGroupOfNodesQuotaAsync(String repositoryName, String workspaceName, String patternPath)
      throws UnknownQuotaLimitException
   {
      patternPath = escaping(patternPath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATTERNS, patternPath);
      return getAsyncUpdate(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public long getNodeQuota(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      nodePath = escaping(nodePath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATHS, nodePath);
      return getQuota(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public long getGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath)
      throws UnknownQuotaLimitException
   {
      patternPath = escaping(patternPath);

      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATTERNS, patternPath);
      return getQuota(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllNodeQuota(String repositoryName, String workspaceName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATHS);
      return getAllChildrenItems(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllGroupOfNodesQuota(String repositoryName, String workspaceName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(QUOTA, repositoryName, workspaceName, QUOTA_PATTERNS);
      return getAllChildrenItems(fqn);
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllTrackedNodes(String repositoryName, String workspaceName)
   {
      Fqn<String> fqn = Fqn.fromRelativeElements(DATA_SIZE, repositoryName, workspaceName);
      return getAllChildrenItems(fqn);
   }

   private Set<String> getAllChildrenItems(final Fqn<String> fqn)
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Set<String>>()
         {
            public Set<String> run() throws Exception
            {
               Set<String> result = new HashSet<String>();
               for (Object path : cache.getChildrenNames(fqn))
               {
                  result.add(unescaping((String)path));
               }
               return result;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   // ==============================> private methods

   /**
    * Returns quota value otherwise throws {@link UnknownQuotaLimitException}.
    */
   private long getQuota(final Fqn<String> fqn) throws UnknownQuotaLimitException
   {
      cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
      Long size = null;
      try
      {
         size = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Long>()
         {
            public Long run() throws Exception
            {
               return (Long)cache.get(fqn, SIZE);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
      if (size == null)
      {
         throw new UnknownQuotaLimitException("Quota was not set early");
      }

      return size;
   }

   private boolean getAsyncUpdate(Fqn<String> fqn) throws UnknownQuotaLimitException
   {
      cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
      Boolean size = (Boolean)cache.get(fqn, ASYNC_UPATE);

      if (size == null)
      {
         throw new UnknownQuotaLimitException("Quota was not set early");
      }

      return size;
   }

   /**
    * Returns data size value otherwise throws {@link UnknownDataSizeException}.
    */
   private long getDataSize(Fqn<String> fqn) throws UnknownDataSizeException
   {
      cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
      Long size = (Long)cache.get(fqn, SIZE);

      if (size == null)
      {
         throw new UnknownDataSizeException("Data size is unknown");
      }

      return size;
   }

   /**
    * Checks if node with give FQN not exists and creates resident node.
    */
   private void createResidentNode(Fqn<String> fqn)
   {
      Node<Serializable, Object> cacheRoot = cache.getRoot();
      if (!cacheRoot.hasChild(fqn))
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cacheRoot.addChild(fqn).setResident(true);
      }
      else
      {
         cache.getNode(fqn).setResident(true);
      }
   }

   private String escaping(String path)
   {
      return path.replace(Fqn.SEPARATOR, "\\");
   }

   private String unescaping(String path)
   {
      return path.replace("\\", Fqn.SEPARATOR);
   }
}
