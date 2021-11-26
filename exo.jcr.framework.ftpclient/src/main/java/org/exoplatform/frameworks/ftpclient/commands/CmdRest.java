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

public class CmdRest extends FtpCommandImpl
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.CmdRest");

   protected String offset;

   public CmdRest(int offset)
   {
      this.offset = String.format("%d", offset);
   }

   public CmdRest(String offset)
   {
      this.offset = offset;
   }

   public int execute()
   {
      try
      {
         // for tests only
         if (offset == null)
         {
            sendCommand(FtpConst.Commands.CMD_REST);
            return getReply();
         }

         sendCommand(String.format("%s %s", FtpConst.Commands.CMD_REST, offset));
         return getReply();
      }
      catch (Exception exc)
      {
         LOG.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }
      return -1;
   }

}
