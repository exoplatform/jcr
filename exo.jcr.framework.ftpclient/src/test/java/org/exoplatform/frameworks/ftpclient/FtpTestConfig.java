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

package org.exoplatform.frameworks.ftpclient;

import org.exoplatform.frameworks.ftpclient.client.FtpClientSession;
import org.exoplatform.frameworks.ftpclient.client.FtpClientSessionImpl;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpTestConfig
{

   /*
    * Server location
    */

   public static final String FTP_HOST = "localhost";

   public static final int FTP_PORT = 2121;

   /*
    * Credentials and workspaceName
    */

   public static final String USER_ID = "admin";

   public static final String USER_PASS = "admin";

   public static final String TEST_WORKSPACE = "production";

   /*
    * MultiThread
    */

   public static final int CLIENTS_COUNT = 20;

   public static final int CLIENT_DEPTH = 2;

   public static final String TEST_FOLDER = "/" + TEST_WORKSPACE + "/crash_test2";

   public static FtpClientSession getTestFtpClient()
   {
      return new FtpClientSessionImpl(FTP_HOST, FTP_PORT);
   }

}
