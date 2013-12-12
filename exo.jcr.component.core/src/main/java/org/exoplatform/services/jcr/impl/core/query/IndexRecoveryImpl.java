/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.lucene.OfflinePersistentIndex;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeEvent;
import org.exoplatform.services.rpc.TopologyChangeListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 16.02.2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: IndexRetrievalImpl.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class IndexRecoveryImpl implements IndexRecovery, TopologyChangeListener
{

   /**
    * Logger instance for this class.
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.IndexRecoveryImpl");

   /**
    * Buffer size.
    */
   public static final int BUFFER_SIZE = 1024 * 1024;

   /**
    * The service for executing commands on all nodes of cluster.
    */
   protected final RPCService rpcService;

   /**
    * Remote command responsible for getting the list of relative paths of all files from index directory.
    */
   private RemoteCommand getIndexList;

   /**
    * Remote command responsible for getting data of target file.
    */
   private RemoteCommand getIndexFile;

   /**
    * Remote command to switch index between RO/RW state.
    */
   private RemoteCommand changeIndexMode;

   /**
    * Remote command to check if index can be retrieved.
    */
   private RemoteCommand checkIndexReady;

   /**
    * Remote command to check if node responsible for set index online leave the cluster. 
    */
   private RemoteCommand requestForResponsibleToSetIndexOnline;

   /**
    * Indicates that node keep responsible to set index online.
    */
   protected Boolean isResponsibleToSetIndexOnline = false;

   /**
    * Indicates whether current node is in online or offline mode
    */
   protected Boolean isOnline = true;

   protected final SearchManager searchManager;

   /**
    * Constructor IndexRetrieveImpl.
    * 
    * @throws RepositoryConfigurationException 
    */
   public IndexRecoveryImpl(RPCService rpcService, final SearchManager searchManager)
      throws RepositoryConfigurationException
   {
      this.rpcService = rpcService;
      this.searchManager = searchManager;

      final String commandSuffix = searchManager.getWsId() + "-" + (searchManager.parentSearchManager == null);
      final File indexDirectory = searchManager.getIndexDirectory();

      changeIndexMode = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-changeIndexMode-" + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            boolean isOnline = (Boolean)args[0];
            searchManager.setOnline(isOnline, false, false);
            IndexRecoveryImpl.this.isOnline = isOnline;
            return null;
         }
      });

      getIndexList = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getIndexList-" + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            return SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<ArrayList<String>>()
            {
               public ArrayList<String> run() throws IOException
               {
                  int indexDirLen = indexDirectory.getAbsolutePath().length();

                  ArrayList<String> result = new ArrayList<String>();
                  for (File file : DirectoryHelper.listFiles(indexDirectory))
                  {
                     if (!file.isDirectory())
                     {
                        // if parent directory is not "offline" then add this file. Otherwise skip it.
                        if (!file.getParent().endsWith(OfflinePersistentIndex.NAME))
                        {
                           result.add(file.getAbsolutePath().substring(indexDirLen));
                        }
                     }
                  }
                  return result;
               }
            });
         }
      });

      getIndexFile = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getIndexFile-" + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            String filePath = (String)args[0];
            long offset = (Long)args[1];

            RandomAccessFile file = new RandomAccessFile(new File(indexDirectory, filePath), "r");
            try
            {
               file.seek(offset);
               byte[] buffer = new byte[BUFFER_SIZE];
               int len = file.read(buffer);
               if (len == -1)
               {
                  return null;
               }
               else
               {
                  byte[] data = new byte[len];
                  System.arraycopy(buffer, 0, data, 0, len);

                  return data;
               }
            }
            finally
            {
               try
               {
                  file.close();
               }
               catch (IOException e)
               {
                  log.debug("Could not close the file", e);
               }
            }
         }
      });

      requestForResponsibleToSetIndexOnline = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-requestForResponsibleToSetIndexOnline-"
               + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            return isResponsibleToSetIndexOnline;
         }
      });

      checkIndexReady = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-checkIndexIsReady-" + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            // if index is currently online, then it can be retrieved 
            return new Boolean(searchManager.isOnline());
         }
      });

      rpcService.registerTopologyChangeListener(this);
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public List<String> getIndexList() throws RepositoryException
   {
      try
      {
         return (List<String>)rpcService.executeCommandOnCoordinator(getIndexList, true);
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setIndexOffline() throws RepositoryException
   {
      try
      {
         isResponsibleToSetIndexOnline = true;
         rpcService.executeCommandOnCoordinator(changeIndexMode, true, false);
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setIndexOnline() throws RepositoryException
   {
      try
      {
         rpcService.executeCommandOnCoordinator(changeIndexMode, true, true);
         isResponsibleToSetIndexOnline = false;
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getIndexFile(String filePath) throws RepositoryException
   {
      try
      {
         return new RemoteInputStream(filePath);
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.IndexRecovery#checkIndexReady()
    */
   public boolean checkIndexReady() throws RepositoryException
   {
      try
      {
         return (Boolean)rpcService.executeCommandOnCoordinator(checkIndexReady, true);
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e);
      }
      catch (RPCException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Allows to read data from remote machine.
    */
   class RemoteInputStream extends InputStream
   {
      private final String filePath;

      private long fileOffset = 0;

      private int bufferOffset = 0;

      private byte[] buffer;

      RemoteInputStream(String filePath) throws SecurityException, RPCException
      {
         this.filePath = filePath;
         readNext();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int read() throws IOException
      {
         throw new UnsupportedOperationException("RemoteStream.read() method is not supported");
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int available() throws IOException
      {
         return buffer == null ? 0 : buffer.length - bufferOffset;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int read(byte b[]) throws IOException
      {
         if (buffer == null)
         {
            return -1;
         }
         else if (available() == 0)
         {
            try
            {
               readNext();

               if (buffer == null)
               {
                  return -1;
               }
            }
            catch (SecurityException e)
            {
               throw new IOException(e);
            }
            catch (RPCException e)
            {
               throw new IOException(e);
            }
         }

         int len = Math.min(b.length, available());
         System.arraycopy(buffer, bufferOffset, b, 0, len);
         bufferOffset += len;

         return len;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int read(byte b[], int off, int len) throws IOException
      {
         throw new UnsupportedOperationException(
            "RemoteStream.read(byte b[], int off, int len) method is not supported");
      }

      private void readNext() throws SecurityException, RPCException
      {
         this.buffer = (byte[])rpcService.executeCommandOnCoordinator(getIndexFile, true, filePath, fileOffset);
         if (buffer != null)
         {
            this.fileOffset += this.buffer.length;
            this.bufferOffset = 0;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onChange(TopologyChangeEvent event)
   {
      try
      {
         if (rpcService.isCoordinator() && !isOnline)
         {
            new Thread()
            {
               @Override
               public synchronized void run()
               {
                  try
                  {
                     List<Object> results =
                        rpcService.executeCommandOnAllNodes(requestForResponsibleToSetIndexOnline, true);

                     for (Object result : results)
                     {
                        if (result instanceof Boolean)
                        {
                           if ((Boolean)result)
                           {
                              return;
                           }
                        }
                        else
                        {
                           log.error("Result is not an instance of Boolean" + result);
                        }
                     }
                     // node which was responsible for resuming leave the cluster, so resume component
                     log
                        .error("Node responsible for setting index back online seems to leave the cluster. Setting back online.");
                     searchManager.setOnline(true, false, false);
                  }
                  catch (SecurityException e1)
                  {
                     log.error("You haven't privileges to execute remote command", e1);
                  }
                  catch (RPCException e1)
                  {
                     log.error("Exception during command execution", e1);
                  }
                  catch (IOException e2)
                  {
                     log.error("Exception during setting index back online", e2);
                  }
               }
            }.start();
         }
      }
      catch (RPCException e)
      {
         log.error("Can't check if node coordinator or not.");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close()
   {
      rpcService.unregisterCommand(changeIndexMode);
      rpcService.unregisterCommand(getIndexList);
      rpcService.unregisterCommand(getIndexFile);
      rpcService.unregisterCommand(requestForResponsibleToSetIndexOnline);
      rpcService.unregisterCommand(checkIndexReady);

      rpcService.unregisterTopologyChangeListener(this);
   }

}
