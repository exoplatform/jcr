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
package org.exoplatform.frameworks.ftpclient.cmdtests;

import junit.framework.TestCase;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.frameworks.ftpclient.FtpTestConfig;
import org.exoplatform.frameworks.ftpclient.Log;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.commands.CmdList;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdPasv;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class LISTTest extends TestCase
{

   private static Log log = new Log("LISTTest");

   public void testLIST() throws Exception
   {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      {
         CmdList cmdList = new CmdList();
         assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(cmdList));
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
         CmdList cmdList = new CmdList();
         assertEquals(FtpConst.Replyes.REPLY_425, client.executeCommand(cmdList));
      }

      {
         {
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(cmdPasv));
         }

         {
            CmdList cmdList = new CmdList("NotExistFolder");
            assertEquals(FtpConst.Replyes.REPLY_450, client.executeCommand(cmdList));
         }
      }

      {
         {
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(cmdPasv));
         }

         // Normal executing replies sequence 125..226
         // 125 used in List command inside
         {
            CmdList cmdList = new CmdList();
            assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdList));
         }
      }

      client.close();

      log.info("Complete.\r\n");
   }

}
