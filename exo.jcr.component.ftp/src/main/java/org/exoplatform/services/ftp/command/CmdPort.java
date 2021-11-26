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

package org.exoplatform.services.ftp.command;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.data.FtpDataTransiver;
import org.exoplatform.services.ftp.data.FtpDataTransiverImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdPort extends FtpCommandImpl
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.ftp.CmdPort");

   public CmdPort()
   {
      commandName = FtpConst.Commands.CMD_PORT;
   }

   public void run(String[] params) throws IOException
   {
      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_PORT));
         return;
      }

      StringBuilder host = new StringBuilder();
      int port = 0;

      try
      {
         String[] ports = params[1].split(",");
         for (int i = 0; i < 3; i++)
         {
            host.append(ports[i]).append(".");
         }
         host.append(ports[3]);
         port = new Integer(ports[4]) * 256 + new Integer(ports[5]);
      }
      catch (NumberFormatException exc)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_ILLEGAL, "PORT"));
         return;
      }

      try
      {
         FtpDataTransiver dataTransiver =
            new FtpDataTransiverImpl(host.toString(), port, clientSession().getFtpServer().getConfiguration(),
               clientSession());

         clientSession().setDataTransiver(dataTransiver);
         reply(String.format(FtpConst.Replyes.REPLY_200, "Port command success"));
         return;
      }
      catch (Exception exc)
      {
         log.info("Unhandlede exception. " + exc.getMessage(), exc);
      }

      reply(String.format(FtpConst.Replyes.REPLY_500_ILLEGAL, "PORT"));
   }

}
