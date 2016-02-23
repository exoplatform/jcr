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
package org.exoplatform.services.ftp.command;

import org.exoplatform.services.ftp.FtpConst;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdPass extends FtpCommandImpl
{

   public CmdPass()
   {
      commandName = FtpConst.Commands.CMD_PASS;
      isNeedLogin = false;
   }

   public void run(String[] params) throws Exception
   {
      if ((!FtpConst.Commands.CMD_USER.equals(clientSession().getPrevCommand()))
         || (null == clientSession().getPrevParams()))
      {
         reply(String.format(FtpConst.Replyes.REPLY_503_PASS));
         return;
      }

      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_PASS));
         return;
      }

      String pass = params[1];
      clientSession().setPassword(pass);

      reply(String.format(FtpConst.Replyes.REPLY_230, clientSession().getUserName()));
   }

}
