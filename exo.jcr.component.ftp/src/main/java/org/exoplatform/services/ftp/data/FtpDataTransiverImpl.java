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
package org.exoplatform.services.ftp.data;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.ftp.client.FtpClientSession;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.PrivilegedExceptionAction;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpDataTransiverImpl implements FtpDataTransiver
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.FtpDataTransiverImpl");

   private FtpClientSession clientSession;

   private FtpDataChannelManager dataChannelManager;

   private FtpConfig configuration;

   private String host;

   private int dataPort = 0;

   private ServerSocket serverSocket;

   private Socket dataSocket = null;

   private Thread connectionThread;

   private boolean isActive = false;

   private InputStream dataInputStream = null;

   private OutputStream dataOutputStream = null;

   public FtpDataTransiverImpl(FtpDataChannelManager dataChannelManager, int dataPort, FtpConfig configuration,
      FtpClientSession clientSession) throws Exception
   {
      this.clientSession = clientSession;
      this.configuration = configuration;
      this.dataChannelManager = dataChannelManager;
      this.dataPort = dataPort;

      serverSocket = new ServerSocket(dataPort);
      connectionThread = new AcceptDataConnect();
      connectionThread.start();
   }

   public FtpDataTransiverImpl(String host, int dataPort, FtpConfig configuration, FtpClientSession clientSession)
      throws Exception
   {
      this.clientSession = clientSession;
      this.configuration = configuration;
      this.host = host;
      this.dataPort = dataPort;

      isActive = true;

      connectionThread = new ConnectDataPort();
      connectionThread.start();
   }

   public int getDataPort()
   {
      return dataPort;
   }

   public boolean isConnected()
   {
      if (dataSocket == null)
      {
         return false;
      }
      return dataSocket.isConnected();
   }

   public void close()
   {
      if (connectionThread.isAlive())
      {
         try
         {
            connectionThread.stop();
         }
         catch (Exception exc)
         {
            LOG.info("Unhandled exception. " + exc.getMessage(), exc);
         }
      }

      if (serverSocket != null)
      {
         try
         {
            serverSocket.close();
         }
         catch (IOException ioexc)
         {
            LOG.info("Closing socket failure.");
         }

      }

      if (!isActive)
      {
         dataChannelManager.freeDataTransiver(this);
      }
      try
      {
         if (dataSocket != null && dataSocket.isConnected())
         {
            dataSocket.close();
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
   }

   public OutputStream getOutputStream() throws IOException
   {
      if (dataOutputStream == null)
      {
         OutputStream nativeOutputStream = null;

         if (configuration.isNeedTimeOut())
         {
            nativeOutputStream = new FtpTimeStampedOutputStream(dataSocket.getOutputStream(), clientSession);
         }
         else
         {
            nativeOutputStream = dataSocket.getOutputStream();
         }

         if (configuration.isNeedSlowDownLoad())
         {
            dataOutputStream = new FtpSlowOutputStream(nativeOutputStream, configuration.getDownLoadSpeed());
         }
         else
         {
            dataOutputStream = nativeOutputStream;
         }
      }
      return dataOutputStream;
   }

   public InputStream getInputStream() throws IOException
   {
      if (dataInputStream == null)
      {
         InputStream nativeInputStream = null;

         if (configuration.isNeedTimeOut())
         {
            nativeInputStream = new FtpTimeStampedInputStream(dataSocket.getInputStream(), clientSession);
         }
         else
         {
            nativeInputStream = dataSocket.getInputStream();
         }

         if (configuration.isNeedSlowUpLoad())
         {
            dataInputStream = new FtpSlowInputStream(nativeInputStream, configuration.getUpLoadSpeed());
         }
         else
         {
            dataInputStream = nativeInputStream;
         }
      }
      return dataInputStream;
   }

   protected class AcceptDataConnect extends Thread
   {

      public AcceptDataConnect()
      {
         super("AcceptDataConnect"
                  + (configuration.getPortalContainer() == null ? "" : " "
                           + configuration.getPortalContainer().getName()));
      }
      
      protected Log acceptLog = ExoLogger.getLogger("exo.jcr.component.ftp.AcceptDataConnect");

      @Override
      public void run()
      {
         try
         {
            dataSocket = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Socket>()
            {
               public Socket run() throws Exception
               {
                  return serverSocket.accept();
               }
            });

            serverSocket.close();
         }
         catch (Exception exc)
         {
            LOG.info("Unhandled exception. " + exc.getMessage(), exc);
         }
      }

   }

   protected class ConnectDataPort extends Thread
   {

      public ConnectDataPort()
      {
         super("ConnectDataPort"
                  + (configuration.getPortalContainer() == null ? "" : " "
                           + configuration.getPortalContainer().getName()));
      }

      protected Log connectLog = ExoLogger.getLogger("exo.jcr.component.ftp.ConnectDataPort");

      @Override
      public void run()
      {
         try
         {
            dataSocket = new Socket();
            SocketAddress sockAddr = new InetSocketAddress(host, dataPort);
            dataSocket.connect(sockAddr);
         }
         catch (Exception exc)
         {
            LOG.info("Unhandled exception. " + exc.getMessage(), exc);
         }
      }

   }

}
