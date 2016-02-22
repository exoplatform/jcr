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

import org.apache.commons.chain.Context;
import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.FtpContext;
import org.exoplatform.services.ftp.client.FtpClientSession;
import org.exoplatform.services.ftp.listcode.FtpFileInfo;
import org.exoplatform.services.ftp.listcode.FtpFileInfoImpl;
import org.exoplatform.services.ftp.listcode.FtpSystemCoder;
import org.exoplatform.services.ftp.listcode.FtpSystemCoderManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public abstract class FtpCommandImpl implements FtpCommand
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.FtpCommandImpl");

   protected boolean isNeedLogin = true;

   protected String commandName = "";

   public ThreadLocal<FtpClientSession> localClientSession = new ThreadLocal<FtpClientSession>();

   public void reply(String replyString) throws IOException
   {
      clientSession().reply(replyString);
   }

   public boolean execute(Context context) throws Exception
   {
      localClientSession.set(((FtpContext)context).getFtpClientSession());

      if (isNeedLogin)
      {
         if (!clientSession().isLogged())
         {
            reply(FtpConst.Replyes.REPLY_530);
            return false;
         }
      }

      String[] params = ((FtpContext)context).getParams();
      run(params);

      String cmdParams = null;
      if (params.length > 1)
      {
         cmdParams = params[1];
      }
      clientSession().setPrevCommand(commandName);
      clientSession().setPrevParams(cmdParams);
      return true;
   }

   public abstract void run(String[] params) throws Exception;

   public FtpClientSession clientSession()
   {
      return localClientSession.get();
   }

   public ArrayList<FtpFileInfo> getFileList(String resPath)
   {
      ArrayList<FtpFileInfo> files = new ArrayList<FtpFileInfo>();

      FtpFileInfo fi = new FtpFileInfoImpl();
      fi.setName(".");
      files.add(fi);
      fi = new FtpFileInfoImpl();
      fi.setName("..");
      files.add(fi);

      ArrayList<String> newPath = clientSession().getFullPath(resPath);

      try
      {
         if (newPath.size() == 0)
         {
            if (clientSession().getFtpServer().getRepository() == null)
            {
               throw new RepositoryException("Repository can not be retrieved.");
            }
            String[] workspaces = clientSession().getFtpServer().getRepository().getWorkspaceNames();
            for (int i = 0; i < workspaces.length; i++)
            {
               FtpFileInfo fileInfo = new FtpFileInfoImpl();
               fileInfo.setName(workspaces[i]);
               files.add(fileInfo);
            }
         }
         else
         {
            String repoPath = clientSession().getRepoPath(newPath);
            Session curSession = clientSession().getSession(newPath.get(0));
            Node parentNode = (Node)curSession.getItem(repoPath);
            if (parentNode.isNodeType(FtpConst.NodeTypes.NT_FILE))
            {
               files.clear();
               FtpFileInfoImpl fileInfo = new FtpFileInfoImpl();
               fileInfo.initFromNode(parentNode);
               files.add(fileInfo);
            }
            else
            {
               NodeIterator nodeIter = parentNode.getNodes();
               while (nodeIter.hasNext())
               {
                  Node curNode = nodeIter.nextNode();
                  FtpFileInfoImpl fileInfo = new FtpFileInfoImpl();
                  fileInfo.initFromNode(curNode);
                  files.add(fileInfo);
               }
            }
         }
      }
      catch (RepositoryException rexc)
      {
         return null;
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
         return null;
      }
      return files;
   }

   public void removeResource(String resName) throws IOException
   {
      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(resName);
         Session curSession = clientSession().getSession(newPath.get(0));

         String repoPath = clientSession().getRepoPath(newPath);
         Node parentNode = (Node)curSession.getItem(repoPath);

         parentNode.remove();
         curSession.save();

         reply(String.format(FtpConst.Replyes.REPLY_250, commandName));
         return;
      }
      catch (PathNotFoundException pexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + pexc.getMessage());
         }
      }
      catch (NoSuchWorkspaceException wexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + wexc.getMessage());
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      reply(String.format(FtpConst.Replyes.REPLY_550, resName));
   }

   public void SendFileList(String[] params) throws IOException
   {
      String path = "";
      if (params.length > 1)
      {
         path = params[1];

         // ingoring some unknows client options
         if (path.startsWith("-la"))
         {
            path = path.substring(3);
         }

         if (path.startsWith("-a"))
         {
            path = path.substring(2);
         }

         while (path.startsWith(" "))
         {
            path = path.substring(1);
         }
      }

      ArrayList<FtpFileInfo> items = getFileList(path);
      if (items == null)
      {
         reply(String.format(FtpConst.Replyes.REPLY_450, path));
         return;
      }

      if (clientSession().getDataTransiver() == null)
      {
         reply(FtpConst.Replyes.REPLY_425);
         return;
      }

      try
      {
         while (!clientSession().getDataTransiver().isConnected())
         {
            Thread.sleep(100);
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      FtpSystemCoder systemCoder =
         FtpSystemCoderManager.getSystemCoder(clientSession().getFtpServer().getConfiguration());

      reply(FtpConst.Replyes.REPLY_125);

      boolean isNeedExtendedInfo = (commandName.equals(FtpConst.Commands.CMD_LIST)) ? true : false;

      try
      {
         String encoding = clientSession().getFtpServer().getConfiguration().getClientSideEncoding();

         for (int i = 0; i < items.size(); i++)
         {
            FtpFileInfo curFileInfo = items.get(i);

            String curSerialized = "";
            if (isNeedExtendedInfo)
            {
               curSerialized = systemCoder.serializeFileInfo(curFileInfo);
            }
            else
            {
               curSerialized = curFileInfo.getName();
            }

            byte[] data = curSerialized.getBytes(encoding);

            clientSession().getDataTransiver().getOutputStream().write(data);
            clientSession().getDataTransiver().getOutputStream().write("\r\n".getBytes());
         }

         clientSession().closeDataTransiver();
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage());
         LOG.info("Data transmit failed.");
      }
      reply(FtpConst.Replyes.REPLY_226);

   }

}
