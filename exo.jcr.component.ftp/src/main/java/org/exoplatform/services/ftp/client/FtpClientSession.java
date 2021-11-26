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

package org.exoplatform.services.ftp.client;

import org.exoplatform.services.ftp.FtpServer;
import org.exoplatform.services.ftp.data.FtpDataTransiver;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public interface FtpClientSession
{

   FtpServer getFtpServer();

   Socket getClientSocket();

   void reply(String replyString) throws IOException;

   String getServerIp();

   boolean isLogged();

   void logout();

   String getUserName();

   String getUserPassword();

   void setUserName(String userName);

   void setPassword(String userPass) throws Exception;

   FtpDataTransiver getDataTransiver();

   void setDataTransiver(FtpDataTransiver newTransiver);

   void closeDataTransiver();

   String getPrevCommand();

   String getPrevParams();

   String getPrevParamsEx();

   void setPrevCommand(String prevCommand);

   void setPrevParams(String prevParams);

   void setPrevParamsEx(String prevParams);

   String changePath(String resPath);

   ArrayList<String> getPath();

   ArrayList<String> getFullPath(String resPath);

   String getRepoPath(ArrayList<String> repoPath);

   Session getSession(String workspaceName) throws Exception;

   void refreshTimeOut();

}
