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

   protected static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpUtils");

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
