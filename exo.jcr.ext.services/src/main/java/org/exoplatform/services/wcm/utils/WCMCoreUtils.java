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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.utils;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SAS Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com Sep 8, 2009
 */
public class WCMCoreUtils {

  private static final Log LOG = ExoLogger.getLogger(WCMCoreUtils.class.getName());

  /**
   * Gets the system session provider.
   *
   * @return the system session provider
   */
  public static SessionProvider getSystemSessionProvider() {
    SessionProviderService sessionProviderService = ExoContainerContext.getService(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }

  /**
   * Gets the session provider.
   *
   * @return the session provider
   */
  public static SessionProvider getUserSessionProvider() {
    SessionProviderService sessionProviderService = ExoContainerContext.getService(SessionProviderService.class);
    return sessionProviderService.getSessionProvider(null);
  }

  public static boolean isAnonim() {
    ConversationState currentConversationState = ConversationState.getCurrent();
    String userId = currentConversationState == null
                    || currentConversationState.getIdentity() == null ? null : currentConversationState.getIdentity().getUserId();
    return userId == null || IdentityConstants.ANONIM.equals(userId);
  }

  public static SessionProvider createAnonimProvider() {
    return SessionProvider.createAnonimProvider();
  }

  /**
   * Get the current repository
   *
   * @return the current manageable repository
   */
  public static ManageableRepository getRepository() {
    try {
      RepositoryService repositoryService = ExoContainerContext.getService(RepositoryService.class);
      return repositoryService.getCurrentRepository();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("getRepository() failed because of ", e);
      }
    }
    return null;
  }

  public static Node getReferencedContent(SessionProvider sessionProvider,
                                          String workspace,
                                          String nodeIdentifier) throws RepositoryException {
    if (workspace == null || nodeIdentifier == null) {
      throw new ItemNotFoundException();
    }
    RepositoryService repositoryService = ExoContainerContext.getService(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    Node content = null;
    try {
      content = session.getNodeByUUID(nodeIdentifier);
    } catch (ItemNotFoundException itemNotFoundException) {
      try {
        content = (Node) session.getItem(nodeIdentifier);
      } catch (Exception exception) {
        content = null;
      }
    }
    return content;
  }

}
