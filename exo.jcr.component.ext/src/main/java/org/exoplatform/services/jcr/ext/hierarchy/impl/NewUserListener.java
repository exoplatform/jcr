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
package org.exoplatform.services.jcr.ext.hierarchy.impl;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh
 * minh.dang@exoplatform.com Nov 15, 2007 11:13:12 AM
 */
public class NewUserListener extends UserEventListener {
  private static final Log           LOG = ExoLogger.getLogger("exo.jcr.component.ext.NewUserListener");

  private final NodeHierarchyCreator nodeHierarchyCreatorService_;

  public NewUserListener(NodeHierarchyCreator nodeHierarchyCreatorService) throws Exception {
    nodeHierarchyCreatorService_ = nodeHierarchyCreatorService;
  }

  public void preDelete(User user) {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      nodeHierarchyCreatorService_.removeUserNode(sessionProvider, user.getUserName());
    } catch (Exception e) {
      LOG.error("An error occurs while removing the user directory of '" + user.getUserName() + "'", e);
    } finally {
      sessionProvider.close();
      sessionProvider = null;
    }
  }
}
