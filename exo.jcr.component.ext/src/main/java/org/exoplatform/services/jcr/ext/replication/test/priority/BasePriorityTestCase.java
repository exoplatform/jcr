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
package org.exoplatform.services.jcr.ext.replication.test.priority;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.ext.replication.ReplicationChannelManager;
import org.exoplatform.services.jcr.ext.replication.WorkspaceDataTransmitter;
import org.exoplatform.services.jcr.ext.replication.test.BaseReplicationTestCase;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BasePriorityTestCase.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class BasePriorityTestCase extends BaseReplicationTestCase
{

   /**
    * Logger.
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.BasePriorityTestCase");

   /**
    * The workspaceDataTransmitter will be used for getting the ChannelManager.
    */
   protected WorkspaceDataTransmitter dataTransmitter;

   /**
    * BasePriorityTestCase constructor.
    * 
    * @param repositoryService
    *          the RepositoryService.
    * @param reposytoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    */
   public BasePriorityTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);

      WorkspaceContainerFacade wContainer =
         ((RepositoryImpl)repository).getWorkspaceContainer(session.getWorkspace().getName());

      dataTransmitter = (WorkspaceDataTransmitter)wContainer.getComponent(WorkspaceDataTransmitter.class);
   }

   /**
    * disconnectClusterNode.
    * 
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer disconnectClusterNode()
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         ReplicationChannelManager channelManager = dataTransmitter.getChannelManager();
         channelManager.setAllowConnect(false);
         channelManager.disconnect();

         channelManager.connect();

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't disconnected node of cluster: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * disconnectClusterNode.
    * 
    * @param id
    *          the changed id
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer disconnectClusterNode(int id)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         ReplicationChannelManager channelManager = dataTransmitter.getChannelManager();
         channelManager.setAllowConnect(false, id);
         channelManager.disconnect();

         channelManager.connect();

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't disconnected node of cluster: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * allowConnect.
    * 
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer allowConnect()
   {
      StringBuffer sb = new StringBuffer();
      try
      {
         ReplicationChannelManager channelManager = dataTransmitter.getChannelManager();
         channelManager.setAllowConnect(true);

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't allowed connect node of cluster: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * allowConnectForced.
    * 
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer allowConnectForced()
   {
      StringBuffer sb = new StringBuffer();
      try
      {
         ReplicationChannelManager channelManager = dataTransmitter.getChannelManager();
         channelManager.setAllowConnect(true);

         channelManager.disconnect();

         channelManager.connect();

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't allowed connect node of cluster: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * isReadOnly.
    * 
    * @param workspaceName
    *          the workspace name
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer isReadOnly(String workspaceName)
   {
      StringBuffer sb = new StringBuffer();
      try
      {

         WorkspaceContainerFacade wsFacade = ((RepositoryImpl)repository).getWorkspaceContainer(workspaceName);
         WorkspaceDataContainer dataContainer =
            (WorkspaceDataContainer)wsFacade.getComponent(WorkspaceDataContainer.class);
         PersistentDataManager dataManager = (PersistentDataManager)wsFacade.getComponent(PersistentDataManager.class);

         if (!dataManager.isReadOnly())
            throw new Exception("The workspace '" + dataContainer.getName() + "' was not read-only");

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Read-only fail ", e);
         sb.append("fail");
      }

      return sb;
   }
}
