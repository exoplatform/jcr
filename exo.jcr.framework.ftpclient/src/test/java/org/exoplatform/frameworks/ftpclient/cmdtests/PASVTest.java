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
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdPasv;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class PASVTest extends TestCase
{

   private static Log log = new Log("PASVTest");

   public void testPASV() throws Exception
   {
      log.info("Test...");

      {
         FtpClientSession client = FtpTestConfig.getTestFtpClient();
         client.connect();

         {
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(cmdPasv));
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
            CmdPasv cmdPasv = new CmdPasv();
            assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(cmdPasv));
         }

         log.info("Waiting for connection...");

         while (true)
         {
            if (client.getDataTransiver().isConnected())
            {
               break;
            }
            Thread.sleep(1);
         }

         log.info("Connected.");

         client.close();
      }

      // // NOW try to get all available channels
      // // then server reply 421
      // {
      //      
      // ArrayList<FtpClientSession> clients = new ArrayList<FtpClientSession>();
      //
      // // configuration params
      // int FTP_MIN_PORT = 32000;
      // int FTP_MAX_PORT = 32100;
      //      
      // int CLIENTS_COUNT = FTP_MAX_PORT - FTP_MIN_PORT + 2;
      //      
      // log.info("Starting CRUSH test...");
      // log.info("CLIENTS COUNT - " + CLIENTS_COUNT);
      // Thread.sleep(2000);
      // log.info("Started...");
      //      
      // for (int i = 0; i < CLIENTS_COUNT; i++) {
      // FtpClientSession curClient = FtpTestConfig.getTestFtpClient();
      // curClient.connect();
      //        
      // clients.add(curClient);
      //
      // {
      // CmdUser cmdUser = new CmdUser(USER_ID);
      // assertEquals(FtpConst.Replyes.REPLY_331, curClient.executeCommand(cmdUser));
      // }
      //        
      // {
      // CmdPass cmdPass = new CmdPass(USER_PASS);
      // assertEquals(FtpConst.Replyes.REPLY_230, curClient.executeCommand(cmdPass));
      // }
      //        
      // CmdPasv cmdPasv = new CmdPasv();
      //        
      // if (i == CLIENTS_COUNT - 1) {
      // assertEquals(FtpConst.Replyes.REPLY_421, curClient.executeCommand(cmdPasv));
      // } else {
      // assertEquals(FtpConst.Replyes.REPLY_227, curClient.executeCommand(cmdPasv));
      // }
      //        
      // }
      //      
      // for (int i = 0; i < CLIENTS_COUNT; i++) {
      // FtpClientSession curClient = clients.get(i);
      // curClient.close();
      // }
      //
      // }

      log.info("Complete.\r\n");
   }

}
