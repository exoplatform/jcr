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

public class CmdSize extends FtpCommandImpl
{

   public static Log log = ExoLogger.getLogger("exo.jcr.framework.command.CmdSize");

   protected String path;

   protected int size = 0;

   public CmdSize(String path)
   {
      this.path = path;
   }

   public int getSize()
   {
      return size;
   }

   public int execute()
   {
      try
      {
         // for tests only
         if (path == null)
         {
            sendCommand(FtpConst.Commands.CMD_SIZE);
            return getReply();
         }

         sendCommand(String.format("%s %s", FtpConst.Commands.CMD_SIZE, path));

         int reply = getReply();

         if (reply == FtpConst.Replyes.REPLY_213)
         {
            String descr = getDescription();
            String sizeVal = descr.substring(descr.indexOf(" ") + 1);
            size = new Integer(sizeVal);
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
