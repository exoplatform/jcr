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
package org.exoplatform.services.ftp;

import org.exoplatform.services.ftp.client.FtpClientSession;
import org.exoplatform.services.ftp.command.FtpCommand;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.ftp.data.FtpDataChannelManager;
import org.exoplatform.services.jcr.core.ManageableRepository;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public interface FtpServer
{

   boolean start();

   boolean stop();

   boolean unRegisterClient(FtpClientSession clientSession);

   FtpConfig getConfiguration();

   FtpDataChannelManager getDataChannelManager();

   int getClientsCount();

   /**
    * 
    * @return {@link ManageableRepository} or <code>null</code> if repository can not be retrieved
    */
   ManageableRepository getRepository();

   FtpCommand getCommand(String commandName);

}
