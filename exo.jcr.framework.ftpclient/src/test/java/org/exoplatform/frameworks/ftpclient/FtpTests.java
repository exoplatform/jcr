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
import junit.framework.TestSuite;

import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.cmdtests.CDUPTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.CWDTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.DELETest;
import org.exoplatform.frameworks.ftpclient.cmdtests.HELPTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.LISTTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.MKDTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.MODETest;
import org.exoplatform.frameworks.ftpclient.cmdtests.NLSTTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.NOOPTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.PASVTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.PORTTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.PWDTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.QUITTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.RESTTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.RETRTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.RMDTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.RNFRTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.RNTOTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.SIZETest;
import org.exoplatform.frameworks.ftpclient.cmdtests.STATTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.STORTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.STRUTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.SYSTTest;
import org.exoplatform.frameworks.ftpclient.cmdtests.TYPETest;
import org.exoplatform.frameworks.ftpclient.cmdtests.USERPASSTest;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpTests extends TestCase
{

   public static TestSuite suite()
   {
      Log log = new Log("FtpTests");

      log.info("Checking server...");

      TestSuite suite = new TestSuite("jcr.ftp tests");

      log.info("checking...");
      if (!isServerPresent())
      {
         log.info("Server not found! Tests are skipping...");
         return suite;
      }

      log.info("Preparing FTP tests...");

      suite.addTestSuite(NOOPTest.class);
      suite.addTestSuite(HELPTest.class);
      suite.addTestSuite(QUITTest.class);
      suite.addTestSuite(USERPASSTest.class);
      suite.addTestSuite(MODETest.class);
      suite.addTestSuite(TYPETest.class);
      suite.addTestSuite(SYSTTest.class);
      suite.addTestSuite(STRUTest.class);
      suite.addTestSuite(STATTest.class);
      suite.addTestSuite(PWDTest.class);
      suite.addTestSuite(CWDTest.class);
      suite.addTestSuite(CDUPTest.class);
      suite.addTestSuite(MKDTest.class);
      suite.addTestSuite(RMDTest.class);
      suite.addTestSuite(DELETest.class);
      suite.addTestSuite(PASVTest.class);
      suite.addTestSuite(PORTTest.class);
      suite.addTestSuite(LISTTest.class);
      suite.addTestSuite(NLSTTest.class);
      suite.addTestSuite(SIZETest.class);
      suite.addTestSuite(RNFRTest.class);
      suite.addTestSuite(RNTOTest.class);
      suite.addTestSuite(RESTTest.class);
      suite.addTestSuite(STORTest.class);
      suite.addTestSuite(RETRTest.class);

      return suite;
   }

   private static boolean isServerPresent()
   {
      try
      {
         FtpClientSession client = FtpTestConfig.getTestFtpClient();
         boolean connected = client.connect();

         if (connected)
         {
            client.close();
            return true;
         }

      }
      catch (Exception exc)
      {
      }

      return false;
   }

}
