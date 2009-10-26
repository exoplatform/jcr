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
package org.exoplatform.services.jcr.ext.initializer;

import java.io.File;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 17.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RemoteTransport.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public interface RemoteTransport
{

   /**
    * Will be initialized the transport.
    * 
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    */
   void init() throws RemoteWorkspaceInitializationException;

   /**
    * Will be closed the transport.
    * 
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    */
   void close() throws RemoteWorkspaceInitializationException;

   /**
    * sendWorkspaceData.
    * 
    * @param workspaceData
    *          the File with workspace data
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    * @throws NoMemberToSendException
    *           will be generated the NoMemberToSendException
    */
   void sendWorkspaceData(File workspaceData) throws RemoteWorkspaceInitializationException, NoMemberToSendException;

   /**
    * getWorkspaceData.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param id
    *          the channel id
    * @return File with workspace data
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    */
   File getWorkspaceData(String repositoryName, String workspaceName, String id)
      throws RemoteWorkspaceInitializationException;

   /**
    * sendError.
    * 
    * @param message
    *          the error message
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    * @throws NoMemberToSendException
    *           will be generated the NoMemberToSendException
    */
   void sendError(String message) throws RemoteWorkspaceInitializationException, NoMemberToSendException;
}
