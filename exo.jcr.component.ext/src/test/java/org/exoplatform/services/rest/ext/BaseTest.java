/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.services.rest.ext;

import junit.framework.TestCase;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.rest.ext.groovy.GroovyJaxrsPublisher;
import org.exoplatform.services.rest.impl.ApplicationContextImpl;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.ResourceLauncher;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public abstract class BaseTest extends TestCase
{
   protected StandaloneContainer container;

   protected ProviderBinder providers;

   protected ResourceBinder binder;

   protected RequestHandlerImpl requestHandler;

   protected GroovyJaxrsPublisher groovyPublisher;

   protected ResourceLauncher launcher;

   public void setUp() throws Exception
   {
      StandaloneContainer.setConfigurationPath("src/test/resources/conf/standalone/test-configuration.xml");
      container = StandaloneContainer.getInstance();
      binder = (ResourceBinder)container.getComponentInstanceOfType(ResourceBinder.class);
      requestHandler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      // reset providers to be sure it is clean
      ProviderBinder.setInstance(new ProviderBinder());
      providers = ProviderBinder.getInstance();
      ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, providers));
      binder.clear();
      groovyPublisher = (GroovyJaxrsPublisher)container.getComponentInstanceOfType(GroovyJaxrsPublisher.class);
      launcher = new ResourceLauncher(requestHandler);
   }

   public void tearDown() throws Exception
   {
   }

}