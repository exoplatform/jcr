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

package org.exoplatform.frameworks.ftpclient.cmdtests;

import junit.framework.TestCase;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.frameworks.ftpclient.FtpTestConfig;
import org.exoplatform.frameworks.ftpclient.Log;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdRest;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class RESTTest extends TestCase
{

   private static Log log = new Log("RESTTest");

   public void testREST() throws Exception
   {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      {
         CmdRest cmdRest = new CmdRest(-1);
         assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(cmdRest));
      }

      {
         CmdUser cmdUser = new CmdUser(FtpTestConfig.USER_ID);
         assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(cmdUser));
      }

      {
         CmdPass cmdPass = new CmdPass(FtpTestConfig.USER_PASS);
         assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(cmdPass));
      }

      {
         CmdRest cmdRest = new CmdRest(null);
         assertEquals(FtpConst.Replyes.REPLY_500, client.executeCommand(cmdRest));
      }

      {
         CmdRest cmdRest = new CmdRest("notoffset");
         assertEquals(FtpConst.Replyes.REPLY_500, client.executeCommand(cmdRest));
      }

      {
         CmdRest cmdRest = new CmdRest("100");
         assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(cmdRest));
      }

      {
         CmdRest cmdRest = new CmdRest(200);
         assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(cmdRest));
      }

      client.close();
      log.info("Complete.\r\n");
   }

}
