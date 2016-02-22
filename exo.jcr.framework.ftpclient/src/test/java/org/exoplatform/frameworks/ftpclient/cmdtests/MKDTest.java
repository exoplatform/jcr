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
import org.exoplatform.frameworks.ftpclient.commands.CmdCwd;
import org.exoplatform.frameworks.ftpclient.commands.CmdMkd;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdRmd;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class MKDTest extends TestCase
{

   private static Log log = new Log("MKDTest");

   public void testMKD() throws Exception
   {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      {
         CmdMkd cmdMkd = new CmdMkd("");
         assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(cmdMkd));
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
         CmdMkd cmdMkd = new CmdMkd("");
         assertEquals(FtpConst.Replyes.REPLY_500, client.executeCommand(cmdMkd));
      }

      {
         CmdMkd cmdMkd = new CmdMkd("myfolder");
         assertEquals(FtpConst.Replyes.REPLY_550, client.executeCommand(cmdMkd));
      }

      {
         CmdCwd cmdCwd = new CmdCwd("production");
         assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));

         String folderName = "test_folder_" + System.currentTimeMillis();

         CmdMkd cmdMkd = new CmdMkd(folderName);
         assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdMkd));

         CmdRmd cmdRmd = new CmdRmd(folderName);
         assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdRmd));
      }

      client.close();
      log.info("Complete.\r\n");
   }
   
   public void testForbiddenCharsMKD() throws Exception {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      {
        CmdUser cmdUser = new CmdUser(FtpTestConfig.USER_ID);
        assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(cmdUser));
      }

      {
        CmdPass cmdPass = new CmdPass(FtpTestConfig.USER_PASS);
        assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(cmdPass));
      }

      {
        CmdCwd cmdCwd = new CmdCwd("production");
        assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));

        String folderName = "test_folder_123" + ":[]*'\"|" + "123";

        CmdMkd cmdMkd = new CmdMkd(folderName);
        assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdMkd));
        
        CmdRmd cmdRmd = new CmdRmd("test_folder_123" + "_______" + "123");
        assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdRmd));
      }

      client.close();
      log.info("Complete.\r\n");
    }

}
