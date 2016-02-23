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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdMode extends FtpCommandImpl
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.CmdMode");

   private String mode;

   public CmdMode(String mode)
   {
      this.mode = mode;
   }

   public int execute()
   {
      try
      {
         sendCommand(String.format("%s %s", FtpConst.Commands.CMD_MODE, mode));
         return getReply();
      }
      catch (Exception exc)
      {
         LOG.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }
      return -1;
   }

}
