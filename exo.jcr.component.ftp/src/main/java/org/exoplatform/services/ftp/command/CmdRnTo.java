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

package org.exoplatform.services.ftp.command;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;

import javax.jcr.Item;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class CmdRnTo extends FtpCommandImpl
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.CmdRnTo");

   public CmdRnTo()
   {
      commandName = FtpConst.Commands.CMD_RNTO;
   }

   public void run(String[] params) throws IOException
   {
      if ((!FtpConst.Commands.CMD_RNFR.equals(clientSession().getPrevCommand()))
         || (clientSession().getPrevParamsEx() == null))
      {
         reply(FtpConst.Replyes.REPLY_503);
         return;
      }

      if (params.length < 2)
      {
         reply(String.format(FtpConst.Replyes.REPLY_500_PARAMREQUIRED, FtpConst.Commands.CMD_RNTO));
         return;
      }

      String pathName = params[1];

      try
      {
         ArrayList<String> newPath = clientSession().getFullPath(pathName);
         Session curSession = clientSession().getSession(newPath.get(0));

         String repoPath = clientSession().getRepoPath(newPath);

         if (curSession.itemExists(repoPath))
         {
            reply(String.format(FtpConst.Replyes.REPLY_553, clientSession().getPrevParamsEx()));
            return;
         }

         // now check does move executed on same workspace
         ArrayList<String> prevParamPath = clientSession().getFullPath(clientSession().getPrevParamsEx());
         String prevRepoPath = clientSession().getRepoPath(prevParamPath);
         if (prevParamPath.get(0).equals(newPath.get(0)))
         {
            //its the same workspace
            curSession.move(prevRepoPath, repoPath);
            curSession.save();
         }
         else
         {
            // there is different workspaces operation
            curSession.getWorkspace().copy(prevParamPath.get(0), prevRepoPath, repoPath);
            // now remove source node
            Session srcSession = clientSession().getSession(prevParamPath.get(0));
            Item item = srcSession.getItem(prevRepoPath);
            item.remove();
            srcSession.save();
         }

         reply(String.format(FtpConst.Replyes.REPLY_250, FtpConst.Commands.CMD_RNTO));
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
         LOG.info("Unhandled exceprion. " + exc.getMessage(), exc);
      }

      reply(String.format(FtpConst.Replyes.REPLY_550, pathName));
   }

}
