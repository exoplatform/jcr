/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.ftp.usecases;

import org.exoplatform.services.ftp.BaseFtpTest;
import org.exoplatform.services.ftp.FtpConst;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestCopyBetweenWorkspaces.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestCopyBetweenWorkspaces extends BaseFtpTest
{

   public void testCopyBetweenWorkspaces() throws Exception
   {
      String filename = "testCopy.txt";
      String fileContent = "exo ftp test copy server test\n";
      try
      {
         connect();
         pwd();
         cwd("ws");
         pwd();
         stor(fileContent.getBytes(), filename);
         String retrieved = new String(retr(filename));
         assertEquals(fileContent, retrieved);

         // make move
         sendCommand(FtpConst.Commands.CMD_RNFR + ' ' + filename);
         String response = readResponse();
         assertTrue("Must start with 350, but was [" + response + "]", response.startsWith("350 "));
         sendCommand(FtpConst.Commands.CMD_RNTO + ' ' + "/ws2/" + filename);
         response = readResponse();
         assertTrue("Must start with 250, but was [" + response + "]", response.startsWith("250 "));

      }
      finally
      {
         disconnect();
      }
   }
}
