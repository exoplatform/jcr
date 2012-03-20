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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.ftp.config.FtpConfigImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class FtpServiceImpl implements FtpService, Startable
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.FtpServiceImpl");

   private CommandService commandService = null;

   private RepositoryService repositoryService = null;

   private FtpServer ftpServer = null;

   private FtpConfig config = null;

   public FtpServiceImpl(InitParams params, CommandService commandService, RepositoryService repositoryService,
      ExoContainerContext context)
   {

      this.commandService = commandService;
      this.repositoryService = repositoryService;
      config = new FtpConfigImpl(context, params);
   }

   public void start()
   {
      LOG.info("Start service.");
      try
      {
         ftpServer = new FtpServerImpl(config, commandService, repositoryService);
         ftpServer.start();
      }
      catch (Exception e)
      {
         LOG.info("Unhandled exception. could not get repository!!!! " + e.getMessage(), e);
      }
   }

   public void stop()
   {
      LOG.info("Stopping...");
      if (ftpServer != null)
         ftpServer.stop();
      else
         LOG.warn("Service isn't started");
   }

}
