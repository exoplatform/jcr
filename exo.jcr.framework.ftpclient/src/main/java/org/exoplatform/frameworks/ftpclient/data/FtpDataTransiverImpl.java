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
package org.exoplatform.frameworks.ftpclient.data;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class FtpDataTransiverImpl implements FtpDataTransiver
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.FtpDataTransiverImpl");

   protected Socket dataSocket = null;

   protected Thread connectionThread;

   public FtpDataTransiverImpl()
   {
   }

   public void OpenPassive(String host, int port)
   {
      connectionThread = new PassiveThread(host, port);
      connectionThread.start();
   }

   public boolean OpenActive(int port)
   {
      try
      {
         connectionThread = new ActiveThread(port);
         connectionThread.start();
         return true;
      }
      catch (Exception exc)
      {
         LOG.info("Can't open active mode. PORT is busy. " + exc.getMessage(), exc);
      }
      return false;
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
      try
      {
         if (connectionThread != null)
         {
            connectionThread.stop();
         }
         if (dataSocket != null && dataSocket.isConnected())
         {
            dataSocket.close();
         }
      }
      catch (Exception exc)
      {
         LOG.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }
   }

   public byte[] receive()
   {
      if (dataSocket == null)
      {
         return null;
      }

      ByteArrayOutputStream outStream = new ByteArrayOutputStream();

      try
      {
         byte[] buffer = new byte[4096];
         while (dataSocket.isConnected())
         {
            int readed = dataSocket.getInputStream().read(buffer);
            if (readed < 0)
            {
               break;
            }
            outStream.write(buffer, 0, readed);
            Thread.sleep(10);
         }
      }
      catch (SocketException exc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + exc.getMessage());
         }
      }
      catch (Exception exc)
      {
         LOG.error(exc.getLocalizedMessage(), exc);
      }

      try
      {
         if (dataSocket.isConnected())
         {
            dataSocket.close();
         }
      }
      catch (IOException exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      return outStream.toByteArray();
   }

   public boolean send(byte[] data)
   {
      if (dataSocket != null)
      {
         try
         {
            dataSocket.getOutputStream().write(data);
            dataSocket.close();
            return true;
         }
         catch (IOException exc)
         {
            LOG.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
         }
      }
      return false;
   }

   protected class PassiveThread extends Thread
   {

      private Log passiveLog = ExoLogger.getLogger("exo.jcr.framework.command.PassiveThread");

      protected String host;

      protected int port;

      public PassiveThread(String host, int port)
      {
         this.host = host;
         this.port = port;
      }

      public void run()
      {
         dataSocket = new Socket();
         SocketAddress sockAddr = new InetSocketAddress(host, port);

         try
         {
            dataSocket.connect(sockAddr);
         }
         catch (Exception exc)
         {
            passiveLog.info("Can't open PASSIVE mode. " + exc.getMessage(), exc);
         }

      }

   }

   protected class ActiveThread extends Thread
   {

      private Log activeLog = ExoLogger.getLogger("exo.jcr.framework.command.ActiveThread");

      protected int port;

      protected ServerSocket serverSocket;

      public ActiveThread(int port) throws Exception
      {
         this.port = port;
         serverSocket = new ServerSocket(port);
      }

      public void run()
      {
         try
         {
            dataSocket = serverSocket.accept();
            serverSocket.close();
         }
         catch (Exception exc)
         {
            activeLog.info("Can't open ACTIVE mode. " + exc.getMessage(), exc);
         }

      }

   }

}
