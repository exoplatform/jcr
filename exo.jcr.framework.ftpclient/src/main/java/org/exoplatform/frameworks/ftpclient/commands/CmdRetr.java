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

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class CmdRetr extends FtpCommandImpl
{

   private static Log log = ExoLogger.getLogger("exo.jcr.framework.command.CmdRetr");

   protected String path;

   protected byte[] fileContent = null;

   public CmdRetr(String path)
   {
      this.path = path;
   }

   public byte[] getFileContent()
   {
      return fileContent;
   }

   public int execute()
   {
      try
      {
         // for tests only
         if (path == null)
         {
            sendCommand(FtpConst.Commands.CMD_RETR);
            return getReply();
         }

         sendCommand(String.format("%s %s", FtpConst.Commands.CMD_RETR, path));

         int reply = getReply();
         if (reply == FtpConst.Replyes.REPLY_125)
         {
            fileContent = clientSession.getDataTransiver().receive();
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
