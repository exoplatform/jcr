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
package org.exoplatform.services.ftp.client;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.FtpContext;
import org.exoplatform.services.ftp.command.FtpCommand;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.Socket;
import java.net.SocketException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class FtpClientCommandThread extends Thread
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpClientCommandThread");

   protected FtpClientSession clientSession;

   public FtpClientCommandThread(FtpClientSession clientSession)
   {
      this.clientSession = clientSession;
   }

   public void run()
   {

      String portalContainerName = clientSession.getFtpServer().getConfiguration().getPortalContainerName();

      ExoContainer container = ExoContainerContext.getContainerByName(portalContainerName);
      if (container == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Container " + portalContainerName + " not found.");
         }
         container = ExoContainerContext.getTopContainer();
      }

      ExoContainerContext.setCurrentContainer(container);

      while (true)
      {
         try
         {
            String command = readLine();

            if (command == null)
            {
               break;
            }

            if (!"".equals(command))
            {
               String logStr = "";
               String[] comms = command.split(" ");

               FtpCommand curCommand = clientSession.getFtpServer().getCommand(comms[0].toUpperCase());

               logStr = comms[0].toUpperCase();

               if (curCommand != null)
               {
                  if (comms.length > 1)
                  {
                     for (int i = 2; i < comms.length; i++)
                     {
                        if ("".equals(comms[i]))
                        {
                           comms[1] += " ";
                        }
                        else
                        {
                           comms[1] += " " + comms[i];
                        }
                     }

                     logStr += " " + comms[1];
                  }

                  FtpContext ftpContext = new FtpContext(clientSession, comms);
                  curCommand.execute(ftpContext);
               }
               else
               {
                  clientSession.reply(String.format(FtpConst.Replyes.REPLY_500, comms[0].toUpperCase()));
                  clientSession.setPrevCommand(null);
               }

            }
         }
         catch (SocketException exc)
         {
            break;
         }
         catch (Exception exc)
         {
            log.info("Unhandled exception. " + exc.getMessage(), exc);
            break;
         }
      }

      try
      {
         clientSession.logout();
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      ExoContainerContext.setCurrentContainer(null);

   }

   protected String readLine() throws Exception
   {
      int[] buffer = new int[4 * 1024];
      int bufPos = 0;
      byte prevByte = 0;

      Socket clientSocket = clientSession.getClientSocket();

      while (true)
      {
         int received = clientSocket.getInputStream().read();
         if (received < 0)
         {
            return null;
         }

         clientSession.refreshTimeOut();

         buffer[bufPos] = (byte)received;
         bufPos++;

         if (prevByte == '\r' && received == '\n')
         {
            byte[] commandLine = new byte[bufPos - 2];
            for (int i = 0; i < bufPos - 2; i++)
            {
               commandLine[i] = (byte)buffer[i];
            }

            try
            {
               String encoding = clientSession.getFtpServer().getConfiguration().getClientSideEncoding();
               String readyCommand = new String(commandLine, encoding);

               if (log.isDebugEnabled())
               {
                  log.debug("FTP_CMD:[" + readyCommand + "]");
               }

               return readyCommand;
            }
            catch (Exception exc)
            {
               log.info("Unahdled exception. " + exc.getMessage());
               exc.printStackTrace();
            }
         }

         prevByte = (byte)received;
      }
   }

}
