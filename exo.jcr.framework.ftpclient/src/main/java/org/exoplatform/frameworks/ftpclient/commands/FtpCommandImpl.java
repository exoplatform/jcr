/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.frameworks.ftpclient.commands;

import org.exoplatform.frameworks.ftpclient.FtpUtils;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public abstract class FtpCommandImpl implements FtpCommand
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.FtpCommandImpl");

   protected FtpClientSession clientSession;

   // protected PrintStream outPrintStream;

   protected int replyCode;

   protected String descript = "";

   public int run(FtpClientSession clientSession)
   {
      this.clientSession = clientSession;

      // outPrintStream = new PrintStream(clientSession.getClientSocket().getOutputStream());
      int status = execute();
      return status;
   }

   public abstract int execute();

   public void sendCommand(String command)
   {
      LOG.info(">>> " + command);

      try
      {
         byte[] data = command.getBytes();
         OutputStream outStream = clientSession.getClientSocket().getOutputStream();
         outStream.write(data);
         outStream.write("\r\n".getBytes());
      }
      catch (IOException exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
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
      LOG.info("try get reply..........");
      StringBuilder reply = new StringBuilder(); 
      String curReply = "";

      while (true)
      {
         curReply = readLine();

         if ("".equals(curReply))
         {
            reply.append("\r\n");
         }
         else
         {
            reply.append(curReply);
            if (isReplyString(curReply))
            {
               break;
            }
            else
            {
               reply.append("\r\n");
            }
         }

      }

      descript = reply.toString();

      replyCode = FtpUtils.getReplyCode(curReply);
      LOG.info("<<< " + descript);
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
            StringBuilder resultLine = new StringBuilder();
            for (int i = 0; i < bufPos - 2; i++)
            {
               resultLine.append((char)buffer[i]);
            }
            return resultLine.toString();
         }

         prevByte = (byte)received;
      }
   }

}
