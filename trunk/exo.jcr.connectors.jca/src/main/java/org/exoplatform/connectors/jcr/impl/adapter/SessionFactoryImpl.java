/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.connectors.jcr.impl.adapter;

import org.exoplatform.connectors.jcr.adapter.SessionFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 * The default implementation of the {@link SessionFactory}.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class SessionFactoryImpl implements SessionFactory
{

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SessionFactoryImpl");

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = 6643308860647346790L;

   /**
    * The reference
    */
   private Reference reference;

   /**
    * The connection manager
    */
   private final ConnectionManager cm;

   /**
    * The managed session factory
    */
   private final ManagedSessionFactory msf;

   /**
    * The default constructor
    */
   SessionFactoryImpl(ConnectionManager cm, ManagedSessionFactory msf)
   {
      this.cm = cm;
      this.msf = msf;
   }

   /**
    * @see javax.resource.Referenceable#setReference(javax.naming.Reference)
    */
   public void setReference(Reference reference)
   {
      this.reference = reference;
   }

   /**
    * @see javax.naming.Referenceable#getReference()
    */
   public Reference getReference() throws NamingException
   {
      return reference;
   }

   /**
    * @see org.exoplatform.connectors.jcr.adapter.SessionFactory#getSession()
    */
   public Session getSession() throws RepositoryException
   {
      return getSession(null);
   }

   /**
    * @see org.exoplatform.connectors.jcr.adapter.SessionFactory#getSession(java.lang.String, java.lang.String)
    */
   public Session getSession(String userName, String password) throws RepositoryException
   {
      return getSession(null, userName, password);
   }

   /**
    * @see org.exoplatform.connectors.jcr.adapter.SessionFactory#getSession(java.lang.String)
    */
   public Session getSession(String workspace) throws RepositoryException
   {
      return getSession(workspace, null, null);
   }

   /**
    * @see org.exoplatform.connectors.jcr.adapter.SessionFactory#getSession(java.lang.String, java.lang.String, java.lang.String)
    */
   public Session getSession(String workspace, String userName, String password) throws RepositoryException
   {
      SessionRequestInfo sri = new SessionRequestInfo(workspace, userName, password);
      if (LOG.isDebugEnabled())
      {
         LOG.debug("getSession " + sri);
      }
      try
      {
         return (Session)cm.allocateConnection(msf, sri);
      }
      catch (ResourceException e)
      {
         throw new RepositoryException("Could not allocate a connection", e);
      }
   }
}
