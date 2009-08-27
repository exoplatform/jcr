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
package org.exoplatform.services.jcr.webdav;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.rest.ext.provider.HierarchicalPropertyEntityProvider;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Common configuration mechanism for WebDAV application. From JAX-RS
 * specification, section 2.1 <br />
 * 
 * <pre>
 * The resources and providers that make up a JAX-RS application are configured
 * via an application-supplied subclass of Application. An implementation MAY
 * provide alternate mechanisms for locating resource classes and providers
 * (e.g. runtime class scanning) but use of Application is the only portable
 * means of configuration.
 * </pre>
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public final class WebDAVApplication extends Application
{

   private Set<Class<?>> classes = new HashSet<Class<?>>();

   private Set<Object> singletons = new HashSet<Object>();

   /**
    * This constructor will be used by third part RESTful frameworks that not use
    * exo container directly.
    * 
    * @see #getPortalContainerName()
    */
   public WebDAVApplication()
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      if (container instanceof RootContainer)
         container = RootContainer.getInstance().getPortalContainer(getPortalContainerName());
      // singleton
      singletons.add(container.getComponentInstanceOfType(WebDavService.class));
      // per-request
      classes.add(HierarchicalPropertyEntityProvider.class);
   }

   /**
    * This constructor will be used by exo container.
    * 
    * @param webdavService WebDavService
    */
   public WebDAVApplication(WebDavService webdavService)
   {
      singletons.add(webdavService);
      classes.add(HierarchicalPropertyEntityProvider.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Class<?>> getClasses()
   {
      return classes;
   }

   /**
    * {@inheritDoc}
    */
   public Set<Object> getSingletons()
   {
      return singletons;
   }

   /**
    * Override this if you need other container name. This method should be used
    * to set container name when this class is not component of exo container.
    * 
    * @return portal container name
    */
   protected String getPortalContainerName()
   {
      return "portal";
   }

}
