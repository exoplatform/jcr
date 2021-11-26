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

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdType extends FtpCommandImpl
{

   public CmdType()
   {
      commandName = FtpConst.Commands.CMD_TYPE;
   }

   public void run(String[] params) throws IOException
   {
      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_TYPE));
         return;
      }

      String typeVal = params[1];
      if ("A".equals(typeVal.toUpperCase()) || "I".equals(typeVal.toUpperCase()))
      {
         reply(String.format(FtpConst.Replyes.REPLY_200, "Type set to " + typeVal.toUpperCase()));
         return;
      }

      reply(String.format(FtpConst.Replyes.REPLY_500, "'" + FtpConst.Commands.CMD_TYPE + " " + params[1].toUpperCase()
         + "'"));
   }

}
