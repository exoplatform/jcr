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
package org.exoplatform.services.ftp;

import org.apache.commons.chain.Catalog;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.ftp.client.FtpClientSession;
import org.exoplatform.services.ftp.client.FtpClientSessionImpl;
import org.exoplatform.services.ftp.command.FtpCommand;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.ftp.data.FtpDataChannelManager;
import org.exoplatform.services.ftp.data.FtpDataChannelManagerImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpServerImpl implements FtpServer
{

   private static final Log LOG = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpServerImpl");

   public static final String COMMAND_PATH = "/conf/ftp-commands.xml";

   private Catalog commandCatalog;

   private RepositoryService repositoryService;

   private FtpConfig configuration;

   private FtpAcceptThread acceptThread;

   private FtpDataChannelManager dataChannelManager;

   private ArrayList<FtpClientSession> clients = new ArrayList<FtpClientSession>();

   public FtpServerImpl(FtpConfig configuration, CommandService commandService, RepositoryService repositoryService)
      throws Exception
   {
      this.configuration = configuration;
      this.repositoryService = repositoryService;

      InputStream commandStream = SecurityHelper.doPrivilegedAction(new PrivilegedAction<InputStream>()
      {
         public InputStream run()
         {
            return getClass().getResourceAsStream(COMMAND_PATH);
         }
      });

      commandService.putCatalog(commandStream);
      commandCatalog = commandService.getCatalog(FtpConst.FTP_COMMAND_CATALOG);
   }

   protected void prepareCache()
   {
      String cacheFolderName = configuration.getCacheFolderName();

      File cacheFolder = new File(cacheFolderName);

      if (!PrivilegedFileHelper.exists(cacheFolder))
      {
         LOG.info("Cache folder not exist. Try to create it...");
         PrivilegedFileHelper.mkdirs(cacheFolder);
      }

      String[] cacheFiles = PrivilegedFileHelper.list(cacheFolder);
      if (cacheFiles == null)
      {
         LOG.info("No cache file in cache folder!");
         return;
      }

      for (String cacheFile : cacheFiles)
      {
         if (cacheFile.endsWith(FtpConst.FTP_CACHEFILEEXTENTION))
         {
            File file = new File(cacheFolderName + "/" + cacheFile);
            PrivilegedFileHelper.delete(file);
         }
      }

   }

   public boolean start()
   {
      try
      {
         prepareCache();

         ServerSocket serverSocket = null;
         int port = configuration.getCommandPort();
         // Trying to find a port available
         while (serverSocket == null)
         {
            try
            {
               serverSocket = new ServerSocket(port);
               LOG.info("FTPServer started on port '" + port + "'");
            }
            catch (BindException e)
            {
               LOG.warn("Cannot launch the FTPServer on '" + (port++) + "', we try the next port number");
            }
         }

         dataChannelManager = new FtpDataChannelManagerImpl(configuration);

         acceptThread = new FtpAcceptThread(this, serverSocket);
         acceptThread.start();

         return true;
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      return false;
   }

   public boolean stop()
   {
      return false;
   }

   public FtpConfig getConfiguration()
   {
      return configuration;
   }

   public ManageableRepository getRepository()
   {

      try
      {
         return repositoryService.getDefaultRepository();
      }
      catch (RepositoryException e)
      {
         LOG.info("Repository exception. " + e.getMessage(), e);
      }
      catch (RepositoryConfigurationException e)
      {
         LOG.info("Repository configuration exception. " + e.getMessage(), e);
      }

      return null;
   }

   public FtpCommand getCommand(String commandName)
   {
      return (FtpCommand)commandCatalog.getCommand(commandName);
   }

   public FtpDataChannelManager getDataChannelManager()
   {
      return dataChannelManager;
   }

   public boolean unRegisterClient(FtpClientSession clientSession)
   {
      boolean result = clients.remove(clientSession);
      LOG.info(">>> Client disconnected. Clients: " + clients.size());
      return result;
   }

   public int getClientsCount()
   {
      return clients.size();
   }

   protected class FtpAcceptThread extends Thread
   {

      protected FtpServer ftpServer;

      protected ServerSocket serverSocket;

      protected boolean enable = true;

      public FtpAcceptThread(FtpServer ftpServer, ServerSocket serverSocket)
      {
         this.ftpServer = ftpServer;
         this.serverSocket = serverSocket;
         setName("Ftp Server"
                  + (configuration.getPortalContainer() == null ? "" : " "
                           + configuration.getPortalContainer().getName()));
         setDaemon(true);
      }

      public void disable()
      {
         enable = false;
      }

      @Override
      public void run()
      {
         while (enable)
         {
            Socket incoming = null;
            try
            {
               incoming = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Socket>()
               {
                  public Socket run() throws Exception
                  {
                     return serverSocket.accept();
                  }
               });

               FtpClientSession clientSession = new FtpClientSessionImpl(ftpServer, incoming);
               clients.add(clientSession);

               LOG.info(">>> New client connected. Clients: " + clients.size());
            }
            catch (Exception exc)
            {
               LOG.info("Unhandled exception. " + exc.getMessage(), exc);
            }
         }
      }

   }

}
