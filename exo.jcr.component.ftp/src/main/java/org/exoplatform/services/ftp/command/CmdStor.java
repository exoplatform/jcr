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

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.FtpTextUtils;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: CmdStor.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class CmdStor extends FtpCommandImpl
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.CmdStor");

   public CmdStor()
   {
      commandName = FtpConst.Commands.CMD_STOR;
   }

   public void run(String[] params) throws IOException
   {
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

      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_STOR));
         return;
      }

      String fileName = params[1];

      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(fileName);
         
         FtpConfig ftpConfig = clientSession().getFtpServer().getConfiguration();
         if (ftpConfig.isReplaceForbiddenChars())
         {
            String fName = newPath.get(newPath.size()-1);
            String newfName =
                     FtpTextUtils.replaceForbiddenChars(fName, ftpConfig.getForbiddenChars(), ftpConfig
                              .getReplaceChar());
            
            fileName  = fileName.substring(0, fileName.indexOf(fName)) + newfName;
         }
         
         Session curSession = clientSession().getSession(newPath.get(0));

         Node resourceNode = getExistedFileNode(curSession, fileName);

         boolean isNeedCheckIn = false;
         if (resourceNode == null)
         {
            if (FtpConst.Commands.CMD_REST.equals(clientSession().getPrevCommand()))
            {
               reply(String.format(FtpConst.Replyes.REPLY_550, "Requested file not exist"));
               return;
            }

            resourceNode = createNewFileNode(curSession, fileName);

         }
         else
         {
            if (FtpConst.Commands.CMD_REST.equals(clientSession().getPrevCommand()))
            {
               int restOffset = new Integer(clientSession().getPrevParams());

               if (restOffset > (resourceNode.getProperty(FtpConst.NodeTypes.JCR_DATA).getLength() + 1))
               {
                  reply(String.format(FtpConst.Replyes.REPLY_550, "Restore value invalid"));
                  return;
               }

            }

            if (resourceNode.getParent().isNodeType(FtpConst.NodeTypes.MIX_VERSIONABLE))
            {
               resourceNode.getParent().checkout();
               isNeedCheckIn = true;
            }
         }

         InputStream inputStream = null;
         String cacheFileName = null;
         if (FtpConst.Commands.CMD_REST.equals(clientSession().getPrevCommand()))
         {
            String cacheFolderName = clientSession().getFtpServer().getConfiguration().getCacheFolderName();
            cacheFileName = cacheFolderName + "/" + IdGenerator.generate() + FtpConst.FTP_CACHEFILEEXTENTION;

            File cacheFile = new File(cacheFileName);
            boolean created = cacheFile.createNewFile();
            if (!created)
            {
               reply(String.format(FtpConst.Replyes.REPLY_550, "STOR"));
               return;
            }

            FileOutputStream cacheOutStream = new FileOutputStream(cacheFile);
            InputStream nodeInputStream = resourceNode.getProperty(FtpConst.NodeTypes.JCR_DATA).getStream();

            if (nodeInputStream == null)
            {
               reply(String.format(FtpConst.Replyes.REPLY_550, "STOR"));
               return;
            }

            byte[] buffer = new byte[32 * 1024];

            while (true)
            {
               int readed = nodeInputStream.read(buffer);
               if (readed < 0)
               {
                  break;
               }
               cacheOutStream.write(buffer, 0, readed);
            }

            cacheOutStream.close();

            RandomAccessFile cacheFilePoint = new RandomAccessFile(cacheFile, "rw");
            int restOffset = new Integer(clientSession().getPrevParams());

            cacheFilePoint.seek(restOffset);

            InputStream socketInputStream = clientSession().getDataTransiver().getInputStream();

            reply(FtpConst.Replyes.REPLY_125);

            while (true)
            {
               int readed = socketInputStream.read(buffer);
               if (readed < 0)
               {
                  break;
               }
               cacheFilePoint.write(buffer, 0, readed);
            }

            cacheFilePoint.close();

            inputStream = new FileInputStream(cacheFile);

         }
         else
         {
            inputStream = clientSession().getDataTransiver().getInputStream();
            reply(FtpConst.Replyes.REPLY_125);
         }

         resourceNode.setProperty(FtpConst.NodeTypes.JCR_LASTMODIFIED, Calendar.getInstance());

         resourceNode.setProperty(FtpConst.NodeTypes.JCR_DATA, inputStream);

         curSession.save();

         clientSession().closeDataTransiver();

         try
         {
            inputStream.close();
         }
         catch (IOException exc)
         {
            LOG.info("Failurinc closing input stream");
         }

         if (cacheFileName != null)
         {
            File cacheFile = new File(cacheFileName);
            cacheFile.delete();
         }

         if (isNeedCheckIn)
         {
            resourceNode.getParent().checkin();
         }

         reply(FtpConst.Replyes.REPLY_226);
         return;
      }
      catch (RepositoryException rexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + rexc.getMessage());
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      clientSession().closeDataTransiver();
      reply(String.format(FtpConst.Replyes.REPLY_550, fileName));
   }

   protected Node getExistedFileNode(Session curSession, String resName)
   {
      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(resName);
         String repoPath = clientSession().getRepoPath(newPath);

         Node fileNode = (Node)curSession.getItem(repoPath);

         return fileNode.getNode(FtpConst.NodeTypes.JCR_CONTENT);
      }
      catch (RepositoryException rexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + rexc.getMessage());
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      return null;
   }

   protected Node createNewFileNode(Session curSession, String resName)
   {
      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(resName);

         String repoPath = clientSession().getRepoPath(newPath);
         String onlyName = repoPath.substring(repoPath.lastIndexOf("/") + 1);
         String onlyPath = repoPath.substring(0, repoPath.length() - onlyName.length());

         if (onlyPath.length() > 1)
         {
            if (onlyPath.endsWith("/"))
            {
               onlyPath = onlyPath.substring(0, onlyPath.length() - 1);
            }
         }

         Node parentNode = (Node)curSession.getItem(onlyPath);

         FtpConfig configuration = clientSession().getFtpServer().getConfiguration();

         String fileNodeType = configuration.getDefFileNodeType();

         Node fileNode = parentNode.addNode(onlyName, fileNodeType);
         Node dataNode = fileNode.addNode(FtpConst.NodeTypes.JCR_CONTENT, FtpConst.NodeTypes.NT_RESOURCE);

         MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();
         mimeTypeResolver.setDefaultMimeType(configuration.getDefFileMimeType());
         String mimeType = mimeTypeResolver.getMimeType(onlyName);

         dataNode.setProperty(FtpConst.NodeTypes.JCR_MIMETYPE, mimeType);

         dataNode.setProperty(FtpConst.NodeTypes.JCR_LASTMODIFIED, Calendar.getInstance());

         return dataNode;
      }
      catch (PathNotFoundException pexc)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + pexc.getMessage());
         }
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      return null;
   }

}
