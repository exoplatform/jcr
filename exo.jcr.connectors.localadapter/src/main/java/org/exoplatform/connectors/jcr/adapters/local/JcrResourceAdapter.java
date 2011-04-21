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
package org.exoplatform.connectors.jcr.adapters.local;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.MalformedURLException;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:lautarul@gmail.com">Roman Pedchenko</a>
 * @version $Id: JcrResourceAdapter.java 7176 2006-07-19 07:59:47Z peterit $
 */

public class JcrResourceAdapter implements ResourceAdapter
{

   private static Log log = ExoLogger.getLogger("exo.jcr.connectors.localadapter.JcrResourceAdapter");

   String containerConfig;

   /*
    * (non-Javadoc)
    * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
    */
   public synchronized void start(BootstrapContext ctx) throws ResourceAdapterInternalException
   {

      log.info("<<<<<<<<<<<<<<<<<< JcrResourceAdapter.start(), " + containerConfig + " >>>>>>>>>>>>>>>>>>>");

      log.info("Container config: " + containerConfig);
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      try
      {

         if (containerConfig != null && containerConfig.length() > 0)
         {
            String url = Thread.currentThread().getContextClassLoader().getResource(containerConfig).toString();
            StandaloneContainer.addConfigurationURL(url);
         }
      }
      catch (MalformedURLException e)
      {
         log.warn("Invalid containerConfig URL, ignored: " + containerConfig, e);
      }

      try
      {
         StandaloneContainer sc = StandaloneContainer.getInstance();
      }
      catch (Exception e)
      {
         log.error("Standalone container start error: " + e, e);
      }
   }

   /*
    * (non-Javadoc)
    * @see javax.resource.spi.ResourceAdapter#stop()
    */
   public void stop()
   {
      log.info("<<<<<<<<<<<<<<<<<< JcrResourceAdapter.stop(), " + containerConfig + " >>>>>>>>>>>>>>>>>>>");
      try
      {
         StandaloneContainer sc = StandaloneContainer.getInstance();
         sc.stop();
      }
      catch (Exception e)
      {
         log.error("Standalone container stop error: " + e, e);
      }
   }

   /*
    * (non-Javadoc)
    * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
    */
   public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException
   {
      return null;
   }

   /*
    * (non-Javadoc)
    * @seejavax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.
    * MessageEndpointFactory, javax.resource.spi.ActivationSpec)
    */
   public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException
   {
   }

   /*
    * (non-Javadoc)
    * @seejavax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.
    * MessageEndpointFactory, javax.resource.spi.ActivationSpec)
    */
   public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
   {
   }

   public void setContainerConfig(String prop)
   {
      this.containerConfig = prop;
   }

}
