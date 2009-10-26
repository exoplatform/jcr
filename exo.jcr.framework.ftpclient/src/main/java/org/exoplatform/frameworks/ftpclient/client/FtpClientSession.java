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
