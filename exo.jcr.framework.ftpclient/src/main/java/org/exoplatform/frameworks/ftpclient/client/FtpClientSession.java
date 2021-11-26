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

package org.exoplatform.frameworks.ftpclient.client;

import org.exoplatform.frameworks.ftpclient.commands.FtpCommand;
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiver;

import java.net.Socket;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public interface FtpClientSession
{

   Socket getClientSocket();

   boolean connect() throws Exception;

   boolean connect(int attemptsCount) throws Exception;

   void close();

   int executeCommand(FtpCommand command) throws Exception;

   int executeCommand(FtpCommand command, int expectReply, int attemptsCount) throws Exception;

   void setSystemType(String systemType);

   String getSystemType();

   void setDataTransiver(FtpDataTransiver dataTransiver);

   FtpDataTransiver getDataTransiver();

}
