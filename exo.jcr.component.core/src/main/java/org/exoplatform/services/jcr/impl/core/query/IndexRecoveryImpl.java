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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
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
public class IndexRecoveryImpl implements IndexRecovery
{

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
   private RemoteCommand setReadOnly;

   /**
    * Constructor IndexRetrieveImpl.
    * 
    * @throws RepositoryConfigurationException 
    */
   public IndexRecoveryImpl(RPCService rpcService, final SearchManager searchManager)
      throws RepositoryConfigurationException
   {
      this.rpcService = rpcService;

      final String commandSuffix = searchManager.getWsId() + "-" + (searchManager.parentSearchManager == null);
      final File indexDirectory = searchManager.getIndexDirectory();

      setReadOnly = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-setReadOnly-" + commandSuffix;
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            boolean isReadOnly = (Boolean)args[0];

            // TODO searchManager.setReadOnly(isReadOnly);

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
            int indexDirLen = PrivilegedFileHelper.getAbsolutePath(indexDirectory).length();

            ArrayList<String> result = new ArrayList<String>();
            for (File file : DirectoryHelper.listFiles(indexDirectory))
            {
               if (!file.isDirectory())
               {
                  result.add(PrivilegedFileHelper.getAbsolutePath(file).substring(indexDirLen));
               }
            }

            return result;
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
            int offset = (Integer)args[1];

            RandomAccessFile file = new RandomAccessFile(new File(indexDirectory, filePath), "r");
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
      });
   }

   /**
    * {@inheritDoc}
    */
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
   public void setIndexReadOnly(boolean isReadOnly) throws RepositoryException
   {
      try
      {
         rpcService.executeCommandOnCoordinator(setReadOnly, true, isReadOnly);

         // TODO failover
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
    * Allows to read data from remote machine.
    */
   class RemoteInputStream extends InputStream
   {
      private final String filePath;

      private int fileOffset = 0;

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
         throw new UnsupportedOperationException(
            "RemoteStream.read(byte b[], int off, int len) method is not supported");
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
}
