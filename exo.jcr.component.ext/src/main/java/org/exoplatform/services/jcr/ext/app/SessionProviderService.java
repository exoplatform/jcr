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
package org.exoplatform.services.jcr.ext.app;

import javax.jcr.RepositoryException;

import org.exoplatform.container.*;
import org.exoplatform.container.spi.DefinitionByType;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS . Session providers holder component
 * 
 * @author Gennady Azarenkov
 * @version $Id: SessionProviderService.java 13869 2008-05-05 08:40:10Z
 *          pnedonosko $
 */
@DefinitionByType(type = ThreadLocalSessionProviderService.class)
public interface SessionProviderService {

  /**
   * @param key
   * @param sessionProvider
   */
  void setSessionProvider(Object key, SessionProvider sessionProvider);

  /**
   * @param key
   * @return session provider
   */
  SessionProvider getSessionProvider(Object key);

  /**
   * @param key
   * @return system session provider
   */
  SessionProvider getSystemSessionProvider(Object key);

  /**
   * Removes the session provider
   * 
   * @param key
   */
  void removeSessionProvider(Object key);

  /**
   * Gets the system session provider.
   *
   * @return the system session provider
   */
  public static SessionProvider getSystemSessionProvider() {
    SessionProviderService sessionProviderService = getSessionProviderService();
    return sessionProviderService.getSystemSessionProvider(null);
  }

  /**
   * Get the current repository
   *
   * @return the current manageable repository
   */
  public static ManageableRepository getRepository() {
    try {
      return getRepositoryService().getCurrentRepository();
    } catch (RepositoryException e) {
      return null;
    }
  }

  public static SessionProviderService getSessionProviderService() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (container == null || container.getComponentInstanceOfType(SessionProviderService.class) == null) {
      container = PortalContainer.getInstance();
    }
    return container.getComponentInstanceOfType(SessionProviderService.class);
  }

  public static RepositoryService getRepositoryService() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (container == null || container.getComponentInstanceOfType(RepositoryService.class) == null) {
      container = PortalContainer.getInstance();
    }
    return container.getComponentInstanceOfType(RepositoryService.class);
  }

}
