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
package org.exoplatform.frameworks.ftpclient;

import junit.framework.TestCase;

import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.commands.CmdCdUp;
import org.exoplatform.frameworks.ftpclient.commands.CmdCwd;
import org.exoplatform.frameworks.ftpclient.commands.CmdDele;
import org.exoplatform.frameworks.ftpclient.commands.CmdList;
import org.exoplatform.frameworks.ftpclient.commands.CmdMkd;
import org.exoplatform.frameworks.ftpclient.commands.CmdNlst;
import org.exoplatform.frameworks.ftpclient.commands.CmdNoop;
import org.exoplatform.frameworks.ftpclient.commands.CmdPass;
import org.exoplatform.frameworks.ftpclient.commands.CmdPasv;
import org.exoplatform.frameworks.ftpclient.commands.CmdPort;
import org.exoplatform.frameworks.ftpclient.commands.CmdPwd;
import org.exoplatform.frameworks.ftpclient.commands.CmdRest;
import org.exoplatform.frameworks.ftpclient.commands.CmdRetr;
import org.exoplatform.frameworks.ftpclient.commands.CmdRmd;
import org.exoplatform.frameworks.ftpclient.commands.CmdRnFr;
import org.exoplatform.frameworks.ftpclient.commands.CmdRnTo;
import org.exoplatform.frameworks.ftpclient.commands.CmdSize;
import org.exoplatform.frameworks.ftpclient.commands.CmdStor;
import org.exoplatform.frameworks.ftpclient.commands.CmdSyst;
import org.exoplatform.frameworks.ftpclient.commands.CmdType;
import org.exoplatform.frameworks.ftpclient.commands.CmdUser;
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiver;
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiverImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Random;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class FtpTest extends TestCase
{

   protected static Log getLogger(String unitName)
   {
      return ExoLogger.getLogger("exo.jcr.framework.command." + unitName);
   }

   public void testNOOP() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_NOOP);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_200, client.executeCommand(new CmdNoop()));

      client.close();

      log.info("Complete.");
   }

   public void testUSER() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_USER);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));

      client.close();

      log.info("Complete.");
   }

   public void testPASS() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_PASS);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));

      client.close();

      log.info("Complete.");
   }

   public void testSYST() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_SYST);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));

      log.info("Complete.");
   }

   public void testPWD() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_PWD);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));

      CmdPwd cmdPwd = new CmdPwd();
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));

      log.info("CURRENTPATH - [" + cmdPwd.getCurrentPath() + "]");

      // assertEquals("/", cmdPwd.getCurrentPath());

      log.info("Complete.");
   }

   public void testCWD() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_CWD);
      log.info("Test...");

      // success

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("production")));

      CmdPwd cmdPwd = new CmdPwd();
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
      assertEquals("/production", cmdPwd.getCurrentPath());

      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("../backup/")));

      cmdPwd = new CmdPwd();

      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
      assertEquals("/backup", cmdPwd.getCurrentPath());

      assertEquals(FtpConst.Replyes.REPLY_550, client.executeCommand(new CmdCwd("production")));

      client.close();

      // denied
      client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_530, client.executeCommand(new CmdCwd("production")));

      client.close();

      log.info("Complete.");
   }

   public void testCDUP() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_CDUP);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("production")));

      CmdPwd cmdPwd = new CmdPwd();
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
      assertEquals("/production", cmdPwd.getCurrentPath());

      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCdUp()));

      cmdPwd = new CmdPwd();
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(cmdPwd));
      assertEquals("/", cmdPwd.getCurrentPath());

      client.close();

      log.info("Complete.");
   }

   public void testTYPE() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_TYPE);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));

      assertEquals(FtpConst.Replyes.REPLY_200, client.executeCommand(new CmdType("A")));
      assertEquals(FtpConst.Replyes.REPLY_200, client.executeCommand(new CmdType("I")));
      assertEquals(FtpConst.Replyes.REPLY_500, client.executeCommand(new CmdType("SOMETYPE")));

      client.close();

      log.info("Complete.");
   }

   public void testPASV() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_PASV);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      boolean connected = false;

      for (int i = 0; i < 15; i++)
      {
         try
         {
            if (client.getDataTransiver().isConnected())
            {
               connected = true;
               break;
            }
            Thread.sleep(1000);
         }
         catch (Exception exc)
         {
            log.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
         }
      }

      assertEquals(true, connected);

      client.getDataTransiver().close();

      client.close();

      log.info("Complete.");
   }

   public void testPORT() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_PORT);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));

      int MIN_PORT_VAL = 9000;
      int MAX_PORT_VAL = 10000;

      int ports = MAX_PORT_VAL - MIN_PORT_VAL + 1;

      Random random = new Random();

      String host = "127.0.0.1";
      int port = 0;

      boolean enableSearch = true;
      while (enableSearch)
      {
         port = MIN_PORT_VAL + random.nextInt(ports);
         if (FtpUtils.isPortFree(port))
         {
            enableSearch = false;
         }
      }

      FtpDataTransiver dataTransiver = new FtpDataTransiverImpl();
      dataTransiver.OpenActive(port);
      client.setDataTransiver(dataTransiver);

      try
      {
         Thread.sleep(1000);
      }
      catch (Exception exc)
      {
         log.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
      }

      CmdPort cmdPort = new CmdPort(host, port);

      assertEquals(FtpConst.Replyes.REPLY_200, client.executeCommand(cmdPort));

      boolean connected = false;
      for (int i = 0; i < 15; i++)
      {
         try
         {
            if (client.getDataTransiver().isConnected())
            {
               connected = true;
               break;
            }
            Thread.sleep(1000);
         }
         catch (Exception exc)
         {
            log.info(FtpConst.EXC_MSG + exc.getMessage(), exc);
         }
      }

      assertEquals(true, connected);

      client.getDataTransiver().close();

      client.close();

      log.info("Complete.");
   }

   public void testLIST() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_LIST);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("production")));
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(new CmdList()));

      client.close();

      log.info("Complete.");
   }

   public void testNLST() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_NLST);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdCwd("production")));
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(new CmdNlst()));

      client.close();

      log.info("Complete.");
   }

   public void testMKD() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_MKD);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(new CmdMkd("/production/test_folder")));

      client.close();

      log.info("Complete.");
   }

   public void testRMD() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_RMD);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(new CmdMkd("/production/test_folder")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdRmd("/production/test_folder")));

      client.close();

      log.info("Complete.");
   }

   public void testRNFR_RNTO() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_RNFR + "_" + FtpConst.Commands.CMD_RNTO);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_257, client.executeCommand(new CmdMkd("/production/test_kokanoid_torename")));

      assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(new CmdRnFr("/production/test_kokanoid_torename")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdRnTo("/production/test_kokanoid_renamed")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdRmd("/production/test_kokanoid_renamed")));

      client.close();

      log.info("Complete.");
   }

   protected static final String FILE_CONTENT = "eXo TEST File Content...";

   public void testSTOR() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_STOR);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      CmdStor cmdStor = new CmdStor("/production/test_file.txt");

      byte[] data = new byte[FILE_CONTENT.length()];
      char[] chars = FILE_CONTENT.toCharArray();
      for (int i = 0; i < data.length; i++)
      {
         data[i] = (byte)chars[i];
      }

      cmdStor.setFileContent(data);

      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdStor));

      client.close();

      log.info("Complete.");
   }

   public void testSIZE() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_SIZE);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));

      assertEquals(FtpConst.Replyes.REPLY_213, client.executeCommand(new CmdSize("/production/test_file.txt")));

      // retrieve size here..., assert it

      assertEquals(FtpConst.Replyes.REPLY_550, client.executeCommand(new CmdSize("/production/test_file_absent.txt")));

      client.close();

      log.info("Complete.");
   }

   public void testRETR() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_RETR);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      CmdRetr cmdRetr = new CmdRetr("/production/test_file.txt");
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdRetr));

      byte[] fileContent = cmdRetr.getFileContent();
      assertEquals(fileContent.length, FILE_CONTENT.length());

      for (int i = 0; i < fileContent.length; i++)
      {
         if (fileContent[i] != FILE_CONTENT.charAt(i))
         {
            fail();
         }
      }

      client.close();

      log.info("Complete.");
   }

   public void testREST() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_REST);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(new CmdRest(9)));

      CmdRetr cmdRetr = new CmdRetr("/production/test_file.txt");
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdRetr));

      byte[] fileContent = cmdRetr.getFileContent();
      assertEquals(fileContent.length, FILE_CONTENT.length() - 9);

      for (int i = 0; i < fileContent.length; i++)
      {
         if (fileContent[i] != FILE_CONTENT.charAt(i + 9))
         {
            fail();
         }
      }

      client.close();

      log.info("Complete.");
   }

   public void testDELE() throws Exception
   {
      Log log = getLogger(FtpConst.Commands.CMD_DELE);
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdDele("/production/test_file.txt")));

      client.close();

      log.info("Complete.");
   }

   protected byte[] getBytes(String content)
   {
      byte[] data = new byte[content.length()];
      for (int i = 0; i < data.length; i++)
      {
         data[i] = (byte)content.charAt(i);
      }
      return data;
   }

   public void testREST_STOR() throws Exception
   {
      Log log = getLogger("REST_STOR");
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      assertEquals(FtpConst.Replyes.REPLY_331, client.executeCommand(new CmdUser("admin")));
      assertEquals(FtpConst.Replyes.REPLY_230, client.executeCommand(new CmdPass("admin")));
      assertEquals(FtpConst.Replyes.REPLY_215, client.executeCommand(new CmdSyst()));

      String fileName = "/production/resr_test_file.txt";
      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      byte[] data = getBytes("DATABYTES");

      CmdStor cmdStor = new CmdStor(fileName);
      cmdStor.setFileContent(data);

      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdStor));

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      CmdRetr cmdRetr = new CmdRetr(fileName);
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdRetr));

      byte[] dataAfter = cmdRetr.getFileContent();
      for (int i = 0; i < dataAfter.length; i++)
      {
         if (dataAfter[i] != data[i])
         {
            fail();
         }
      }

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(new CmdRest(data.length)));

      byte[] secondData = getBytes("_APPENDED");

      cmdStor = new CmdStor(fileName);
      cmdStor.setFileContent(secondData);

      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdStor));

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      cmdRetr = new CmdRetr(fileName);
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdRetr));

      String secondString = "DATABYTES_APPENDED";

      byte[] secondDataAfter = cmdRetr.getFileContent();
      for (int i = 0; i < secondDataAfter.length; i++)
      {
         if (secondDataAfter[i] != (byte)secondString.charAt(i))
         {
            fail();
         }
      }

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      assertEquals(FtpConst.Replyes.REPLY_350, client.executeCommand(new CmdRest(4)));

      byte[] replasedData = getBytes("INT");

      cmdStor = new CmdStor(fileName);
      cmdStor.setFileContent(replasedData);

      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdStor));

      assertEquals(FtpConst.Replyes.REPLY_227, client.executeCommand(new CmdPasv()));

      cmdRetr = new CmdRetr(fileName);
      assertEquals(FtpConst.Replyes.REPLY_226, client.executeCommand(cmdRetr));

      String readyString = "DATAINTES_APPENDED";

      byte[] readyBytes = cmdRetr.getFileContent();
      for (int i = 0; i < readyBytes.length; i++)
      {
         if (readyBytes[i] != (byte)readyString.charAt(i))
         {
            fail();
         }
      }

      assertEquals(FtpConst.Replyes.REPLY_250, client.executeCommand(new CmdDele(fileName)));

      client.close();

      log.info("Complete.");
   }

}
