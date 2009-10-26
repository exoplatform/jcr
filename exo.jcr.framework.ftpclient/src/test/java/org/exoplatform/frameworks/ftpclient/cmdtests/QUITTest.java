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
import org.exoplatform.frameworks.ftpclient.commands.CmdQuit;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class QUITTest extends TestCase
{

   private static Log log = new Log("QUITTest");

   public void test_QUIT() throws Exception
   {
      log.info("Test...");

      FtpClientSession client = FtpTestConfig.getTestFtpClient();
      client.connect();

      CmdQuit cmdQuit = new CmdQuit();
      assertEquals(FtpConst.Replyes.REPLY_221, client.executeCommand(cmdQuit));

      client.close();
      log.info("Complete.\r\n");
   }

}
