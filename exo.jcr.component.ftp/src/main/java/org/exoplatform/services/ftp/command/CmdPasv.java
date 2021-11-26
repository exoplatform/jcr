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

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdPasv extends FtpCommandImpl
{

   public CmdPasv()
   {
      commandName = FtpConst.Commands.CMD_PASV;
   }

   public void run(String[] params) throws IOException
   {
      FtpDataTransiver transiver =
         clientSession().getFtpServer().getDataChannelManager().getDataTransiver(clientSession());

      if (transiver == null)
      {
         reply(FtpConst.Replyes.REPLY_421_DATA);
         return;
      }
      clientSession().setDataTransiver(transiver);

      StringBuilder serverLocation = new StringBuilder(clientSession().getServerIp().replace('.', ','));
      int dataPort = transiver.getDataPort();
      int high = dataPort / 256;
      int low = dataPort % 256;

      serverLocation.append(String.format(",%s,%s", high, low));
      reply(String.format(FtpConst.Replyes.REPLY_227, serverLocation.toString()));
   }

}
