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
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class CmdStor extends FtpCommandImpl
{

   private static Log log = ExoLogger.getLogger("exo.jcr.framework.command.CmdStor");

   protected String path;

   protected byte[] fileContent = null;

   public CmdStor(String path)
   {
      this.path = path;
   }

   public void setFileContent(byte[] fileContent)
   {
      this.fileContent = fileContent;
   }

   public int execute()
   {
      if (fileContent == null)
      {
         return -1;
      }

      try
      {
         FtpDataTransiver dataTransiver = clientSession.getDataTransiver();

         if (dataTransiver != null)
         {
            for (int i = 0; i < 150; i++)
            {
               if (!dataTransiver.isConnected())
               {
                  Thread.sleep(100);
               }
            }
         }

         if (path == null)
         {
            sendCommand(FtpConst.Commands.CMD_STOR);
            return getReply();
         }

         sendCommand(String.format("%s %s", FtpConst.Commands.CMD_STOR, path));

         int reply = getReply();
         if (reply == FtpConst.Replyes.REPLY_125)
         {
            dataTransiver.send(fileContent);
            reply = getReply();
         }
         return reply;
      }
      catch (Exception exc)
      {
         log.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }
      return -1;
   }

}
