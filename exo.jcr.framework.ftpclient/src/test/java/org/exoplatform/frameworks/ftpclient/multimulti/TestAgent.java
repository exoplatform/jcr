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
package org.exoplatform.frameworks.ftpclient.multimulti;

import junit.framework.TestCase;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.frameworks.ftpclient.FtpTestConfig;
import org.exoplatform.frameworks.ftpclient.Log;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSessionImpl;
import org.exoplatform.frameworks.ftpclient.commands.CmdCwd;
import org.exoplatform.frameworks.ftpclient.commands.CmdDele;
import org.exoplatform.frameworks.ftpclient.commands.CmdMkd;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdPasv;
import org.exoplatform.frameworks.ftpclient.commands.CmdPwd;
import org.exoplatform.frameworks.ftpclient.commands.CmdRetr;
import org.exoplatform.frameworks.ftpclient.commands.CmdStor;
import org.exoplatform.frameworks.ftpclient.commands.CmdSyst;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class TestAgent extends TestCase
{

   public static final int SLEEP = 100;

   private Log log;

   private int agentId;

   private int itemsCount;

   private ClientThread clientThread;

   private boolean successed = false;
   
   /**
    * the name of the test case
    */
   private String fName;
   
   public TestAgent(String name) {
      fName= name;
   }

   public TestAgent(int agentId, int itemsCount)
   {
      log = new Log("TestAgent[" + agentId + "]");

      this.agentId = agentId;
      this.itemsCount = itemsCount;

      log.info("construct agent with ID [" + agentId + "]");
      clientThread = new ClientThread();
      clientThread.start();
   }

   public boolean isAlive()
   {
      return clientThread.isAlive();
   }

   public boolean isSuccessed()
   {
      return successed;
   }

   private class ClientThread extends Thread
   {

      private Log log = new Log("ClientThread[" + agentId + "]");

      private FtpClientSession client;

      public ClientThread()
      {
      }

      public void run()
      {

         try
         {

            while (MultiThreadCrushTest.IsNeedWaitAll)
            {
               Thread.sleep(1000);
            }

            client = new FtpClientSessionImpl(FtpTestConfig.FTP_HOST, FtpTestConfig.FTP_PORT);

            if (!client.connect(60))
            {
               return;
            }

            {
               CmdUser cmdUser = new CmdUser(FtpTestConfig.USER_ID);
               assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(cmdUser));
            }

            Thread.sleep(SLEEP);

            {
               CmdPass cmdPass = new CmdPass(FtpTestConfig.USER_PASS);
               assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(cmdPass));
            }

            Thread.sleep(SLEEP);

            {
               CmdSyst cmdSyst = new CmdSyst();
               assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(cmdSyst));
            }

            Thread.sleep(SLEEP);

            {
               CmdPwd cmdPwd = new CmdPwd();
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
            }

            Thread.sleep(SLEEP);

            {
               CmdCwd cmdCwd = new CmdCwd(FtpTestConfig.TEST_FOLDER);
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));
            }

            Thread.sleep(SLEEP);

            {
               CmdPwd cmdPwd = new CmdPwd();
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
               assertEquals(FtpTestConfig.TEST_FOLDER, cmdPwd.getCurrentPath());
            }

            Thread.sleep(SLEEP);

            String folderName = "" + agentId;

            String folder1Name = folderName.substring(0, 1);
            log.info("FOLDER 1 NAME: [" + folder1Name + "]");

            {
               CmdCwd cmdCwd = new CmdCwd(folder1Name);
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));
            }

            Thread.sleep(SLEEP);

            String folder2Name = folderName.substring(1, 2);

            {
               CmdCwd cmdCwd = new CmdCwd(folder2Name);
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));
            }

            Thread.sleep(SLEEP);

            {
               CmdMkd cmdMkd = new CmdMkd(folderName);
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdMkd, FtpConst.Replyes.REPLY_257, 3));
            }

            Thread.sleep(SLEEP);

            {
               CmdCwd cmdCwd = new CmdCwd(folderName);
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));
            }

            Thread.sleep(SLEEP);

            String folderPath = FtpTestConfig.TEST_FOLDER + "/" + folder1Name + "/" + folder2Name + "/" + folderName;
            log.info("FOLDER PATH: [" + folderPath + "]");

            {
               CmdPwd cmdPwd = new CmdPwd();
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
               assertEquals(folderPath, cmdPwd.getCurrentPath());
            }

            Thread.sleep(SLEEP);

            for (int i1 = 0; i1 < itemsCount; i1++)
            {

               String tf1Name = "test_folder_" + i1;

               CmdMkd cmdMkd = new CmdMkd(tf1Name);
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdMkd, FtpConst.Replyes.REPLY_257, 3));

               Thread.sleep(SLEEP);

               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd(tf1Name)));

               Thread.sleep(SLEEP);

               for (int i2 = 0; i2 < itemsCount; i2++)
               {
                  String tf2Name = "test_file_" + i2;

                  int expectReply = 3;
                  while (expectReply > 0)
                  {

                     int pasvReply = client.executeCommand(new CmdPasv(), FtpConst.Replyes.REPLY_227, 3);
                     if (pasvReply == FtpConst.Replyes.REPLY_227)
                     {
                        // byte []data = ("TEST FILE CONTENT " + i2).getBytes();
                        byte[] data = new byte[20 * 1024];

                        CmdStor cmdStor = new CmdStor(tf2Name);
                        cmdStor.setFileContent(data);
                        int storReply = client.executeCommand(cmdStor);
                        if (storReply == FtpConst.Replyes.REPLY_226)
                        {
                           break;
                        }
                        Thread.sleep(SLEEP);
                     }

                     Thread.sleep(SLEEP);
                     expectReply--;
                  }

                  if (expectReply == 0)
                  {
                     fail();
                  }
               }

               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("..")));
               Thread.sleep(SLEEP);
            }

            for (int i1 = 0; i1 < itemsCount; i1++)
            {

               String tf1Name = "test_folder_" + i1;

               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd(tf1Name)));
               Thread.sleep(SLEEP);

               for (int i2 = 0; i2 < itemsCount; i2++)
               {
                  String tf2Name = "test_file_" + i2;

                  int expectReply = 3;
                  while (expectReply > 0)
                  {
                     int pasvReply = client.executeCommand(new CmdPasv(), FtpConst.Replyes.REPLY_227, 3);
                     if (pasvReply == FtpConst.Replyes.REPLY_227)
                     {
                        CmdRetr cmdRetr = new CmdRetr(tf2Name);
                        int retrReply = client.executeCommand(cmdRetr);
                        if (retrReply == FtpConst.Replyes.REPLY_226)
                        {
                           break;
                        }
                     }
                  }

                  if (expectReply == 0)
                  {
                     fail();
                  }

                  Thread.sleep(SLEEP);
               }

               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("..")));
            }

            Thread.sleep(SLEEP);

            {
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("..")));
            }

            Thread.sleep(SLEEP);

            {
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdDele(folderName)));
            }

            Thread.sleep(SLEEP);

            client.close();

            successed = true;
         }
         catch (Throwable exc)
         {
            log.info("Unhandled exception. " + exc.getMessage(), exc);
         }

      }

   }

}
