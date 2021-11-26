/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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

package org.exoplatform.frameworks.jcr.web;

import java.util.EnumSet;

import javax.servlet.*;
import javax.servlet.FilterRegistration.Dynamic;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This is a service that is injected via Kernel Container to install a JCR Web
 * Filter. The filter {@link ThreadLocalSessionProviderInitializedFilter} must
 * be injected in the main webapp (portal.war) and rest webapp (rest.war). Since
 * the JCR is an addon, in order to avoid updating the web.xml file of
 * portal.war and rest.war, the filter must be injected dynamically through a
 * {@link ServletContextListener}.
 */
public class InstallJCRServletContextListener implements ServletContextListener {

  private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.InstallJCRServletContextListener");

  @Override
  public void contextInitialized(ServletContextEvent contextEvent) {
    ServletContext context = contextEvent.getServletContext();
    LOG.info("Installing ThreadLocalSessionProviderInitializedFilter into context '{}'", context.getContextPath());

    Dynamic filter = context.addFilter("ThreadLocalSessionProviderInitializedFilter",
                                       ThreadLocalSessionProviderInitializedFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
  }

  @Override
  public void contextDestroyed(ServletContextEvent contextEvent) {
    // Nothing to do
  }

}
