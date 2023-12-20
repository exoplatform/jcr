package org.exoplatform.frameworks.jcr.web;

import java.util.EnumSet;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

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
