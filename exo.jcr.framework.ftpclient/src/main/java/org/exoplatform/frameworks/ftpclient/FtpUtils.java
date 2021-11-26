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

package org.exoplatform.frameworks.ftpclient;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.ServerSocket;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class FtpUtils
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.framework.command.FtpUtils");

   public static int getReplyCode(String reply)
   {
      if (reply.charAt(3) != ' ')
      {
         return -1;
      }
      String replyCodeVal = reply.substring(0, 3);
      return new Integer(replyCodeVal);
   }

   public static boolean isPortFree(int port)
   {
      try
      {
         ServerSocket serverSocket = new ServerSocket(port);
         serverSocket.close();
         return true;
      }
      catch (Exception exc)
      {
         log.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }
      return false;
   }

}
