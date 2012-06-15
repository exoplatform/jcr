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
package org.exoplatform.services.ftp.command;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class CmdRetr extends FtpCommandImpl
{

   private static final Log LOG = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "CmdRetr");

   public CmdRetr()
   {
      commandName = FtpConst.Commands.CMD_RETR;
   }

   public void run(String[] params) throws IOException
   {
      if (clientSession().getDataTransiver() == null)
      {
         reply(FtpConst.Replyes.REPLY_425);
         return;
      }

      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_RETR));
         return;
      }

      String resName = params[1];

      boolean isResource = IsResource(resName);
      if (!isResource)
      {
         reply(String.format(FtpConst.Replyes.REPLY_550, resName));
         return;
      }

      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(resName);
         Session curSession = clientSession().getSession(newPath.get(0));
         String repoPath = clientSession().getRepoPath(newPath);

         Node parentNode = (Node)curSession.getItem(repoPath);
         Node dataNode = parentNode.getNode(FtpConst.NodeTypes.JCR_CONTENT);

         Property dataProp = dataNode.getProperty(FtpConst.NodeTypes.JCR_DATA);

         InputStream inStream = dataProp.getStream();

         if (FtpConst.Commands.CMD_REST.equals(clientSession().getPrevCommand()))
         {
            String prevVal = clientSession().getPrevParams();

            int seekPos = new Integer(prevVal);

            if (seekPos > inStream.available())
            {
               reply(FtpConst.Replyes.REPLY_550_RESTORE);
               return;
            }

            for (int i = 0; i < seekPos; i++)
            {
               inStream.read();
            }
         }

         while (!clientSession().getDataTransiver().isConnected())
         {
            Thread.sleep(100);
         }

         reply(FtpConst.Replyes.REPLY_125);

         int BUFFER_SIZE = 4096;

         try
         {
            byte[] buffer = new byte[BUFFER_SIZE];
            OutputStream outStream = clientSession().getDataTransiver().getOutputStream();

            while (true)
            {
               int readed = inStream.read(buffer, 0, BUFFER_SIZE);
               if (readed < 0)
               {
                  break;
               }
               outStream.write(buffer, 0, readed);
            }

         }
         catch (Exception exc)
         {
            reply(FtpConst.Replyes.REPLY_451);
            return;
         }
         finally
         {
            clientSession().closeDataTransiver();
         }
         reply(FtpConst.Replyes.REPLY_226);
         return;
      }
      catch (Throwable exc) //NOSONAR
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      clientSession().closeDataTransiver();
      reply(String.format(FtpConst.Replyes.REPLY_550, resName));
   }

   public boolean IsResource(String resName)
   {
      ArrayList<String> newPath = clientSession().getFullPath(resName);
      try
      {
         if (!newPath.isEmpty())
         {
            String repoPath = clientSession().getRepoPath(newPath);
            Session curSession = clientSession().getSession(newPath.get(0));

            Node parentNode = (Node)curSession.getItem(repoPath);
            if (parentNode.isNodeType(FtpConst.NodeTypes.NT_FILE))
            {
               return true;
            }
         }
         else
         {
            return false;
         }
      }
      catch (PathNotFoundException exc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + exc.getMessage());
         }
      }
      catch (NoSuchWorkspaceException wexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + wexc.getMessage());
         }
      }
      catch (Throwable exc) //NOSONAR
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      return false;
   }

}
