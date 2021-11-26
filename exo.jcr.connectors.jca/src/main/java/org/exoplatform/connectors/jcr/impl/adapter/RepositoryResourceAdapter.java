/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.connectors.jcr.impl.adapter;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * A dummy ResourceAdapter
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class RepositoryResourceAdapter implements ResourceAdapter
{

   /**
    * {@inheritDoc}
    */
   public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
   {
   }

   /**
    * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
    */
   public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException
   {
      return new XAResource[0];
   }

   /**
    * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
    */
   public void start(BootstrapContext ctx) throws ResourceAdapterInternalException
   {
   }

   /**
    * @see javax.resource.spi.ResourceAdapter#stop()
    */
   public void stop()
   {
   }

   /**
    * {@inheritDoc}
    */
   public int hashCode()
   {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      return this == obj;
   }
}
