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

/**
 * QuotaManager provides methods for getting sizes of all JCR instances, getting/setting quotas and various settings.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: QuotaManager.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public interface QuotaManager
{
   /**
    * Returns a node data size. Size of the node is a size of content stored in it
    * and in all descendant nodes.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param nodePath
    *          the absolute path to node
    * @throws QuotaManagerException If an error occurs.
    */
   long getNodeDataSize(String repositoryName, String workspaceName, String nodePath) throws QuotaManagerException;

   /**
    * Returns a node quota limit, a maximum allowed node data size of this node.
    * First will be tried to check if quota limit was explicitly set for this path.
    * Otherwise will be tried to find pattern quota that matches for defined
    * node path.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param nodePath
    *          the absolute path to node
    * @throws QuotaManagerException If an error occurs.
    * @throws UnknownQuotaLimitException If quota limit was not set
    */
   long getNodeQuota(String repositoryName, String workspaceName, String nodePath) throws QuotaManagerException;

   /**
    * Sets node quota limit, a maximum allowed node data size of this node.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param nodePath
    *          the absolute path to node
    * @param quotaLimit
    *          the maximum allowed node data size of this node
    * @param asyncUpdate
    *          true means updating node data size will be done asynchronously
    * @throws QuotaManagerException If an error occurs.
    */
   void setNodeQuota(String repositoryName, String workspaceName, String nodePath, long quotaLimit, boolean asyncUpdate)
      throws QuotaManagerException;

   /**
    * Removes node quota limit, a maximum allowed node data size of this node.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param nodePath
    *          the absolute path to node
    * @throws QuotaManagerException If an error occurs.
    */
   void removeNodeQuota(String repositoryName, String workspaceName, String nodePath) throws QuotaManagerException;

   /**
    * Sets node quota limit for bunch of nodes at same time.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param patternPath
    *          the pattern indicates bunch of nodes, allowed <code>*</code> as any node name in a path
    *          and <code>%</code> as any character in name
    * @param quotaLimit
    *          the maximum allowed node data size of a node
    * @param asyncUpdate
    *          true means updating node data size will be done asynchronously
    * @throws QuotaManagerException If an error occurs.
    */
   void setGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath, long quotaLimit,
      boolean asyncUpdate) throws QuotaManagerException;

   /**
    * Removes node quota limit for bunch of nodes at same time.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param patternPath
    *          the pattern indicates bunch of nodes, allowed <code>*</code> as any node name in a path
    *          and <code>%</code> as any character in name
    * @throws QuotaManagerException If an error occurs.
    */
   void removeGroupOfNodesQuota(String repositoryName, String workspaceName, String patternPath)
      throws QuotaManagerException;

   /**
    * Returns workspace data size. Size of workspace is a size of content stored in a root node.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @throws QuotaManagerException If an error occurs.
    */
   long getWorkspaceDataSize(String repositoryName, String workspaceName) throws QuotaManagerException;

   /**
    * Returns workspace quota limit, a maximum allowed workspace data size.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @throws QuotaManagerException If an error occurs.
    * @throws UnknownQuotaLimitException If quota limit was not set
    */
   long getWorkspaceQuota(String repositoryName, String workspaceName) throws QuotaManagerException;

   /**
    * Sets workspace quota limit, a maximum allowed workspace data size.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @param quotaLimit
    *          the maximum allowed workspace data size.
    *
    * @throws QuotaManagerException If an error occurs.
    */
   void setWorkspaceQuota(String repositoryName, String workspaceName, long quotaLimit) throws QuotaManagerException;

   /**
    * Removes workspace quota limit, a maximum allowed workspace data size.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @throws QuotaManagerException If an error occurs.
    */
   void removeWorkspaceQuota(String repositoryName, String workspaceName) throws QuotaManagerException;

   /**
    * Returns a index size of particular workspace in repository. Size of the workspace's index is a size of the
    * index directory at file system belonging to workspace.
    *
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name in repository
    * @throws QuotaManagerException If an error occurs.
    */
   long getWorkspaceIndexSize(String repositoryName, String workspaceName) throws QuotaManagerException;

   /**
    * Returns repository data size. Size of repository is a sum of all its workspaces data size.
    *
    * @param repositoryName
    *          the repository name
    * @throws QuotaManagerException If an error occurs.
    */
   long getRepositoryDataSize(String repositoryName) throws QuotaManagerException;

   /**
    * Returns a repository quota limit, a maximum allowed repository data size.
    *
    * @param repositoryName
    *          the repository name
    * @throws QuotaManagerException If an error occurs.
    * @throws UnknownQuotaLimitException If quota limit was not set
    */
   long getRepositoryQuota(String repositoryName) throws QuotaManagerException;

   /**
    * Sets a repository quota limit, a maximum allowed repository data size.
    *
    * @param repositoryName
    *          the repository name
    * @param quotaLimit
    *          the maximum allowed repository data size.
    *
    * @throws QuotaManagerException If an error occurs.
    */
   void setRepositoryQuota(String repositoryName, long quotaLimit) throws QuotaManagerException;

   /**
    * Removes a repository quota limit, a maximum allowed repository data size.
    *
    * @param repositoryName
    *          the repository name
    * @throws QuotaManagerException If an error occurs.
    */
   void removeRepositoryQuota(String repositoryName) throws QuotaManagerException;

   /**
    * Returns a index size of particular repository. Size of the repository's index is a size of the
    * index directory at file system belonging to repository.
    *
    * @param repositoryName
    *          the repository name
    *
    * @throws QuotaManagerException If an error occurs.
    */
   long getRepositoryIndexSize(String repositoryName) throws QuotaManagerException;

   /**
    * Returns global data size. Global size is sum of all repositories data size.
    *
    * @throws QuotaManagerException If an error occurs.
    */
   long getGlobalDataSize() throws QuotaManagerException;

   /**
    * Returns global quota limit, a maximum allowed global data size.
    *
    * @throws QuotaManagerException If an error occurs.
    * @throws UnknownQuotaLimitException If quota limit was not set
    */
   long getGlobalQuota() throws QuotaManagerException;

   /**
    * Sets global quota limit, a maximum allowed global data size.
    *
    * @param quotaLimit
    *          the sum of maximum allowed content size
    *
    * @throws QuotaManagerException If an error occurs.
    */
   void setGlobalQuota(long quotaLimit) throws QuotaManagerException;

   /**
    * Removes global quota limit, a maximum allowed global data size.
    *
    * @throws QuotaManagerException If an error occurs.
    */
   void removeGlobalQuota() throws QuotaManagerException;

   /**
    * Returns a global index size.
    *
    * @throws QuotaManagerException If an error occurs.
    */
   long getGlobalIndexSize() throws QuotaManagerException;
}
