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
import org.exoplatform.frameworks.ftpclient.commands.CmdNlst;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdPasv;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class NLSTTest extends TestCase
{

   private static Log log = new Log("NLSTTest");

   public void testNLST() throws Exception
   {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      {
         CmdNlst cmdNlst = new CmdNlst();
         assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(cmdNlst));
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
         CmdNlst cmdNlst = new CmdNlst();
         assertEquals(FtpConst.Replyes.REPLY_425, client.executeCommand(cmdNlst));
      }

      {
         {
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(cmdPasv));
         }

         {
            CmdNlst cmdNlst = new CmdNlst("NotExistFolder");
            assertEquals(FtpConst.Replyes.REPLY_450, client.executeCommand(cmdNlst));
         }
      }

      {
         {
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(cmdPasv));
         }

         // Normal executing replies sequence 125..226
         // 125 used in NLst command inside
         {
            CmdNlst cmdNlst = new CmdNlst();
            assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdNlst));
         }
      }

      client.close();

      log.info("Complete.\r\n");
   }

}
