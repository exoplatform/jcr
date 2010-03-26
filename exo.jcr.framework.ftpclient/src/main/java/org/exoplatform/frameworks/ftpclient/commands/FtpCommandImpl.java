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
package org.exoplatform.frameworks.ftpclient.commands;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.frameworks.ftpclient.FtpUtils;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public abstract class FtpCommandImpl implements FtpCommand
{

   private static Log log = ExoLogger.getLogger("exo.jcr.framework.command.FtpCommandImpl");

   protected FtpClientSession clientSession;

   // protected PrintStream outPrintStream;

   protected int replyCode;

   protected String descript = "";

   public int run(FtpClientSession clientSession)
   {
      this.clientSession = clientSession;
      try
      {
         // outPrintStream = new PrintStream(clientSession.getClientSocket().getOutputStream());
         int status = execute();
         return status;
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      return -1;
   }

   public abstract int execute();

   public void sendCommand(String command)
   {
      log.info(">>> " + command);

      try
      {
         byte[] data = command.getBytes();
         OutputStream outStream = clientSession.getClientSocket().getOutputStream();
         outStream.write(data);
         outStream.write("\r\n".getBytes());
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }
   }

   private boolean isReplyString(String replyString)
   {
      if (replyString.length() < 4)
      {
         return false;
      }

      if (replyString.charAt(0) >= '0' && replyString.charAt(0) <= '9' && replyString.charAt(1) >= '0'
         && replyString.charAt(1) <= '9' && replyString.charAt(2) >= '0' && replyString.charAt(2) <= '9'
         && replyString.charAt(3) == ' ')
      {
         return true;
      }
      return false;
   }

   public int getReply() throws Exception
   {
      log.info("try get reply..........");
      String reply = "";
      String curReply = "";

      while (true)
      {
         curReply = readLine();

         if ("".equals(curReply))
         {
            reply += "\r\n";
         }
         else
         {
            reply += curReply;
            if (isReplyString(curReply))
            {
               break;
            }
            else
            {
               reply += "\r\n";
            }
         }

      }

      descript = reply;

      replyCode = FtpUtils.getReplyCode(curReply);
      log.info("<<< " + descript);
      return replyCode;
   }

   public String getDescription()
   {
      return descript;
   }

   // public String readLine() throws Exception {
   // BufferedReader br = new BufferedReader(new
   // InputStreamReader(clientSession.getClientSocket().getInputStream()));
   // return br.readLine();
   // }

   public String readLine() throws Exception
   {
      byte[] buffer = new byte[4 * 1024];
      int bufPos = 0;
      byte prevByte = 0;

      while (true)
      {
         int received = clientSession.getClientSocket().getInputStream().read();
         if (received < 0)
         {
            return null;
         }

         buffer[bufPos] = (byte)received;
         bufPos++;

         if (prevByte == '\r' && received == '\n')
         {
            String resultLine = "";
            for (int i = 0; i < bufPos - 2; i++)
            {
               resultLine += (char)buffer[i];
            }
            return resultLine;
         }

         prevByte = (byte)received;
      }
   }

}
