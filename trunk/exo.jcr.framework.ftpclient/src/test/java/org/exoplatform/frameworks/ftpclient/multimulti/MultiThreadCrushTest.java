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
import org.exoplatform.frameworks.ftpclient.commands.CmdSyst;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class MultiThreadCrushTest extends TestCase
{

   private static Log log = new Log("MultiThreadCrushTest");

   public static boolean IsNeedWaitAll = true;

   public void testSingleThread() throws Exception
   {
      log.info("testSingleThread...");

      {
         FtpClientSession client = new FtpClientSessionImpl(FtpTestConfig.FTP_HOST, FtpTestConfig.FTP_PORT);
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
            CmdSyst cmdSyst = new CmdSyst();
            assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(cmdSyst));
         }

         {
            CmdCwd cmdCwd = new CmdCwd(FtpTestConfig.TEST_FOLDER);
            if (FtpConst.Replyes.REPLY_550 == client.executeCommand(cmdCwd))
            {
               CmdMkd cmdMkd = new CmdMkd(FtpTestConfig.TEST_FOLDER);
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdMkd));

               cmdCwd = new CmdCwd(FtpTestConfig.TEST_FOLDER);
               assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(cmdCwd));
            }

            for (int i1 = 0; i1 < 10; i1++)
            {

               String folderName = FtpTestConfig.TEST_FOLDER + "/" + i1;
               assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(new CmdMkd(folderName)));

               for (int i2 = 0; i2 < 10; i2++)
               {
                  String testSubFolder = folderName + "/" + i2;
                  assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(new CmdMkd(testSubFolder)));
               }

            }
         }

         client.close();
      }

      HashMap<Integer, TestAgent> clients = new HashMap<Integer, TestAgent>();
      int count = 0;

      Random random = new Random();

      while (count < FtpTestConfig.CLIENTS_COUNT)
      {
         int nextId = random.nextInt(Integer.MAX_VALUE);
         log.info("NEXT ID: [" + nextId + "]");

         if (nextId < 10000)
         {
            continue;
         }

         Integer curInteger = new Integer(nextId);
         if (clients.containsKey(curInteger))
         {
            continue;
         }

         TestAgent testAgent = new TestAgent(curInteger, FtpTestConfig.CLIENT_DEPTH);
         clients.put(curInteger, testAgent);
         count++;
      }

      log.info("CLIENTS: [" + clients.size() + "]");

      Thread.sleep(3000);
      log.info("START ALL!!!!!!!!!!!!!!!!!!!!!!!!");
      IsNeedWaitAll = false;
      Thread.sleep(3000);

      {
         boolean alive = true;

         while (alive)
         {
            alive = false;
            Thread.sleep(2000);

            int live = 0;

            Iterator<Integer> keyIter = clients.keySet().iterator();
            while (keyIter.hasNext())
            {
               Integer agentKey = keyIter.next();
               TestAgent agent = clients.get(agentKey);
               if (agent.isAlive())
               {
                  alive = true;
                  live++;
               }
            }

            log.info(">>>>>>>>>>>>> LIVE: [" + live + "]");

         }
      }

      {
         {
            FtpClientSession client = new FtpClientSessionImpl(FtpTestConfig.FTP_HOST, FtpTestConfig.FTP_PORT);
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
               CmdSyst cmdSyst = new CmdSyst();
               assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(cmdSyst));
            }

            assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdDele(FtpTestConfig.TEST_FOLDER)));
         }
      }

      {
         int successed = 0;
         Iterator<Integer> keyIter = clients.keySet().iterator();
         while (keyIter.hasNext())
         {
            Integer agentKey = keyIter.next();
            TestAgent agent = clients.get(agentKey);

            if (agent.isSuccessed())
            {
               successed++;
            }

         }

         log.info("SUCCESSED: [" + successed + "]");
         log.info("FAILURES: [" + (FtpTestConfig.CLIENTS_COUNT - successed) + "]");
         Thread.sleep(2000);

         if (FtpTestConfig.CLIENTS_COUNT != successed)
         {
            fail();
         }
      }

      log.info("done.");
   }

}
