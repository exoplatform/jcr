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

package org.exoplatform.services.ftp.data;

import org.exoplatform.services.ftp.client.FtpClientSession;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpDataChannelManagerImpl implements FtpDataChannelManager
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.ftp.FtpDataChannelManagerImpl");

   private FtpConfig configuration;

   private int dataChannels;

   // private Random random;

   protected FtpDataTransiver[] channels;

   public FtpDataChannelManagerImpl(FtpConfig configuration)
   {
      this.configuration = configuration;
      dataChannels = configuration.getDataMaxPort() - configuration.getDataMinPort() + 1;

      channels = new FtpDataTransiver[dataChannels];
   }

   public FtpDataTransiver getDataTransiver(FtpClientSession clientSession)
   {
      synchronized (this)
      {
         for (int i = 0; i < channels.length; i++)
         {
            if (channels[i] == null)
            {
               try
               {
                  FtpDataTransiver transiver =
                     new FtpDataTransiverImpl(this, configuration.getDataMinPort() + i, configuration, clientSession);
                  channels[i] = transiver;
                  return transiver;
               }
               catch (Exception exc)
               {
                  log.info("Unhandled exception. " + exc.getMessage(), exc);
               }
            }
         }
      }
      return null;
   }

   public void freeDataTransiver(FtpDataTransiver dataTransiver)
   {
      synchronized (this)
      {
         int dataPort = dataTransiver.getDataPort();
         int index = dataPort - configuration.getDataMinPort();
         channels[index] = null;
      }
   }

   // public int getFreeDataPort() {
   // int curRandomNum = random.nextInt(dataChannels);
   // //channels[resultChannel - dataMinPort] = 1;
   // return -1;
   // }
   //  
   // public void releaseDataPort(int dataPort) {
   //    
   // }

}
