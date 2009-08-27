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
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiverImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class CmdPasv extends FtpCommandImpl
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "CmdPasv");

   protected String host = "";

   protected int port = 0;

   public int execute()
   {
      try
      {
         sendCommand(FtpConst.Commands.CMD_PASV);

         int reply = getReply();

         if (FtpConst.Replyes.REPLY_227 != reply)
         {
            return reply;
         }

         String descrVal = getDescription();
         descrVal = descrVal.substring(descrVal.indexOf("(") + 1, descrVal.indexOf(")"));

         String[] addrValues = descrVal.split(",");

         host = "";
         for (int i = 0; i < 3; i++)
         {
            host += addrValues[i] + ".";
         }
         host += addrValues[3];

         port = new Integer(addrValues[4]) * 256 + new Integer(addrValues[5]);

         if (FtpConst.Replyes.REPLY_227 == reply)
         {
            FtpDataTransiver dataTransiver = new FtpDataTransiverImpl();
            dataTransiver.OpenPassive(host, port);
            clientSession.setDataTransiver(dataTransiver);
         }

         return reply;
      }
      catch (Exception exc)
      {
         log.info("unhandled ecxeption. " + exc.getMessage(), exc);
      }
      log.info("SOME ERRORS");
      return -1;
   }

   public String getHost()
   {
      return host;
   }

   public int getPort()
   {
      return port;
   }

}
