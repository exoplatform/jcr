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

import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectWriter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractQuotaPersister.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractQuotaPersister implements QuotaPersister
{

   /**
    * Logger.
    */
   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractQuotaPersister");

   /**
    * {@inheritDoc}
    */
   public void setNodeDataSizeIfQuotaExists(String repositoryName, String workspaceName, String nodePath, long dataSize)
   {
      setNodeDataSize(repositoryName, workspaceName, nodePath, dataSize);

      try
      {
         getNodeQuotaOrGroupOfNodesQuota(repositoryName, workspaceName, nodePath);
      }
      catch (UnknownQuotaLimitException e)
      {
         removeNodeDataSize(repositoryName, workspaceName, nodePath);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeQuotaAndDataSize(String repositoryName, String workspaceName, String nodePath)
   {
      removeNodeQuota(repositoryName, workspaceName, nodePath);

      if (getAcceptableGroupOfNodesQuota(repositoryName, workspaceName, nodePath) == null)
      {
         removeNodeDataSize(repositoryName, workspaceName, nodePath);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeGroupOfNodesAndDataSize(String repositoryName, String workspaceName, String patternPath)
   {
      removeGroupOfNodesQuota(repositoryName, workspaceName, patternPath);

      // removes data size for all nodes matched by pattern
      // only if only quota was not set explicitly
      for (String nodePath : getAllTrackedNodes(repositoryName, workspaceName))
      {
         if (PathPatternUtils.acceptName(patternPath, nodePath))
         {
            try
            {
               getNodeQuota(repositoryName, workspaceName, nodePath);
            }
            catch (UnknownQuotaLimitException e)
            {
               removeNodeDataSize(repositoryName, workspaceName, nodePath);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getNodeQuotaOrGroupOfNodesQuota(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      try
      {
         return getNodeQuota(repositoryName, workspaceName, nodePath);
      }
      catch (UnknownQuotaLimitException e)
      {
         String patternPath = getAcceptableGroupOfNodesQuota(repositoryName, workspaceName, nodePath);

         if (patternPath != null)
         {
            return getGroupOfNodesQuota(repositoryName, workspaceName, patternPath);
         }

         throw new UnknownQuotaLimitException("Quota for " + nodePath + " is not defined");
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeQuotaOrGroupOfNodesQuotaAsync(String repositoryName, String workspaceName, String nodePath)
      throws UnknownQuotaLimitException
   {
      try
      {
         return isNodeQuotaAsync(repositoryName, workspaceName, nodePath);
      }
      catch (UnknownQuotaLimitException e)
      {
         String patternPath = getAcceptableGroupOfNodesQuota(repositoryName, workspaceName, nodePath);

         if (patternPath != null)
         {
            return isGroupOfNodesQuotaAsync(repositoryName, workspaceName, patternPath);
         }

         throw new UnknownQuotaLimitException("Quota for " + nodePath + " is not defined");
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getAcceptableGroupOfNodesQuota(String repositoryName, String workspaceName, String nodePath)
   {
      Set<String> patterns = getAllGroupOfNodesQuota(repositoryName, workspaceName);

      for (String patternPath : patterns)
      {
         if (PathPatternUtils.acceptName(patternPath, nodePath))
         {
            return patternPath;
         }
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getAllParentNodesWithQuota(String repositoryName, String workspaceName, String nodePath)
   {
      Set<String> quotableParents = new HashSet<String>();

      for (String quoteablePath : getAllNodeQuota(repositoryName, workspaceName))
      {
         if (nodePath.startsWith(quoteablePath))
         {
            quotableParents.add(quoteablePath);
         }
      }

      for (String pattern : getAllGroupOfNodesQuota(repositoryName, workspaceName))
      {
         if (PathPatternUtils.acceptDescendant(pattern, nodePath))
         {
            String commonAncestor = PathPatternUtils.extractCommonAncestor(pattern, nodePath);
            quotableParents.add(commonAncestor);
         }
      }

      return quotableParents;
   }

   // ===============================================> backupable

   /**
    * {@inheritDoc}
    */
   public void backupWorkspaceData(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws BackupException
   {
      try
      {
         backupWorkspaceDataSize(repositoryName, workspaceName, out);
         backupWorkspaceQuota(repositoryName, workspaceName, out);

         backupAllNodeDataSize(repositoryName, workspaceName, out);

         backupAllNodeQuota(repositoryName, workspaceName, out);
         backupAllGroupOfNodesQuota(repositoryName, workspaceName, out);
      }
      catch (IOException e)
      {
         throw new BackupException(e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreWorkspaceData(String repositoryName, String workspaceName, ZipObjectReader in)
      throws BackupException
   {
      try
      {
         restoreWorkspaceDataSize(repositoryName, workspaceName, in);
         restoreWorkspaceQuota(repositoryName, workspaceName, in);

         restoreAllNodeDataSize(repositoryName, workspaceName, in);

         restoreAllNodeQuota(repositoryName, workspaceName, in);
         restoreAllGroupOfNodesQuota(repositoryName, workspaceName, in);
      }
      catch (IOException e)
      {
         throw new BackupException(e.getMessage(), e);
      }
   }

   private void backupWorkspaceQuota(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws IOException
   {
      out.putNextEntry(new ZipEntry("workspace-quota-limit"));

      try
      {
         long quotaLimit = getWorkspaceQuota(repositoryName, workspaceName);
         out.writeLong(quotaLimit);
      }
      catch (UnknownQuotaLimitException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      out.closeEntry();
   }

   private void backupWorkspaceDataSize(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws IOException
   {
      out.putNextEntry(new ZipEntry("workspace-data-size"));

      try
      {
         long size = getWorkspaceDataSize(repositoryName, workspaceName);
         out.writeLong(size);
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      out.closeEntry();
   }

   private void backupAllNodeDataSize(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws IOException
   {
      out.putNextEntry(new ZipEntry("workspace-nodes-data-size"));

      Set<String> paths = getAllTrackedNodes(repositoryName, workspaceName);
      out.writeInt(paths.size());

      for (String nodePath : paths)
      {
         try
         {
            long size = getNodeDataSize(repositoryName, workspaceName, nodePath);

            out.writeString(nodePath);
            out.writeLong(size);
         }
         catch (UnknownDataSizeException e)
         {
            throw new IOException("Node is tracked but data size is unknown.", e);
         }
      }

      out.closeEntry();
   }

   private void backupAllNodeQuota(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws IOException
   {
      out.putNextEntry(new ZipEntry("workspace-all-node-quota"));

      Set<String> paths = getAllNodeQuota(repositoryName, workspaceName);
      out.writeInt(paths.size());

      for (String nodePath : paths)
      {
         try
         {
            long quotaLimit = getNodeQuota(repositoryName, workspaceName, nodePath);
            boolean async = isNodeQuotaAsync(repositoryName, workspaceName, nodePath);

            out.writeString(nodePath);
            out.writeLong(quotaLimit);
            out.writeBoolean(async);
         }
         catch (UnknownQuotaLimitException e)
         {
            throw new IOException("Node is quoted but qouta limit is unknown.", e);
         }
      }

      out.closeEntry();
   }

   private void backupAllGroupOfNodesQuota(String repositoryName, String workspaceName, ZipObjectWriter out)
      throws IOException
   {
      out.putNextEntry(new ZipEntry("workspace-all-group-of-nodes-quota"));

      Set<String> paths = getAllGroupOfNodesQuota(repositoryName, workspaceName);
      out.writeInt(paths.size());

      for (String patternPath : paths)
      {
         try
         {
            long quotaLimit = getGroupOfNodesQuota(repositoryName, workspaceName, patternPath);
            boolean async = isGroupOfNodesQuotaAsync(repositoryName, workspaceName, patternPath);

            out.writeString(patternPath);
            out.writeLong(quotaLimit);
            out.writeBoolean(async);
         }
         catch (UnknownQuotaLimitException e)
         {
            throw new IOException("Node is quoted but qouta limit is unknown.", e);
         }
      }

      out.closeEntry();
   }

   private void restoreWorkspaceQuota(String repositoryName, String workspaceName, ZipObjectReader in)
      throws IOException
   {
      in.getNextEntry();

      try
      {
         Long quotaLimit = in.readLong();
         setWorkspaceQuota(repositoryName, workspaceName, quotaLimit);
      }
      catch (EOFException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }
   }

   private void restoreWorkspaceDataSize(String repositoryName, String workspaceName, ZipObjectReader in)
      throws IOException
   {
      in.getNextEntry();

      try
      {
         Long dataSize = in.readLong();
         setWorkspaceDataSize(repositoryName, workspaceName, dataSize);
      }
      catch (EOFException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }
   }

   private void restoreAllNodeDataSize(String repositoryName, String workspaceName, ZipObjectReader in)
      throws IOException
   {
      in.getNextEntry();

      int count = in.readInt();
      for (int i = 0; i < count; i++)
      {
         String nodePath = in.readString();
         long dataSize = in.readLong();

         setNodeDataSize(repositoryName, workspaceName, nodePath, dataSize);
      }
   }

   private void restoreAllNodeQuota(String repositoryName, String workspaceName, ZipObjectReader in)
      throws IOException
   {
      in.getNextEntry();

      int count = in.readInt();
      for (int i = 0; i < count; i++)
      {
         String nodePath = in.readString();
         long quotaLimit = in.readLong();
         boolean asyncUpdate = in.readBoolean();

         setNodeQuota(repositoryName, workspaceName, nodePath, quotaLimit, asyncUpdate);
      }
   }

   private void restoreAllGroupOfNodesQuota(String repositoryName, String workspaceName, ZipObjectReader in)
      throws IOException
   {
      in.getNextEntry();

      int count = in.readInt();
      for (int i = 0; i < count; i++)
      {
         String patternPath = in.readString();
         long quotaLimit = in.readLong();
         boolean asyncUpdate = in.readBoolean();

         setGroupOfNodesQuota(repositoryName, workspaceName, patternPath, quotaLimit, asyncUpdate);
      }
   }

}
