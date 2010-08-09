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
package org.exoplatform.frameworks.ftpclient.commands;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.frameworks.ftpclient.data.FtpDataTransiver;
import org.exoplatform.frameworks.ftpclient.data.FtpFileInfo;
import org.exoplatform.frameworks.ftpclient.data.FtpFileInfoImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class CmdList extends FtpCommandImpl
{

   private static Log log = ExoLogger.getLogger("exo.jcr.framework.command.CmdList");

   protected String path = "";

   protected byte[] fileData;

   protected ArrayList<FtpFileInfo> files = new ArrayList<FtpFileInfo>();

   public CmdList()
   {
   }

   public CmdList(String path)
   {
      this.path = path;
   }

   public byte[] getFileData()
   {
      return fileData;
   }

   public ArrayList<FtpFileInfo> getFiles()
   {
      return files;
   }

   public int execute()
   {
      try
      {
         if (clientSession.getSystemType() == null)
         {
            clientSession.executeCommand(new CmdSyst());
         }

         String req;

         if ("".equals(path))
         {
            req = FtpConst.Commands.CMD_LIST;
         }
         else
         {
            req = String.format("%s %s", FtpConst.Commands.CMD_LIST, path);
         }
         sendCommand(req);

         int reply = getReply();

         if (reply == FtpConst.Replyes.REPLY_125 || reply == FtpConst.Replyes.REPLY_150)
         {
            FtpDataTransiver dataTransiver = clientSession.getDataTransiver();

            fileData = dataTransiver.receive();

            dataTransiver.close();

            String dd = new String(fileData, "utf-8");

            String[] lines = dd.split("\r\n");

            String systemType = clientSession.getSystemType();
            systemType = systemType.substring(systemType.indexOf(" ") + 1);

            for (int i = 0; i < lines.length; i++)
            {
               try
               {
                  FtpFileInfo fileInfo = new FtpFileInfoImpl();
                  if (!"".equals(lines[i]))
                  {
                     fileInfo.parseDir(lines[i], systemType);
                     files.add(fileInfo);
                  }
               }
               catch (Exception exc)
               {
                  log.info("CAN'T PARSE FILE LINE: [" + lines[i] + "]");
               }
            }
            reply = getReply();
         }
         return reply;
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      return -1;
   }

}
