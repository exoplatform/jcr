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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 * The implementation of a {@link ManagedConnection} for eXo JCR.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ManagedSessionImpl implements ManagedConnection
{

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ManagedSessionImpl");

   /**
    * The corresponding JCR session
    */
   private final SessionImpl session;

   /**
    * The list of all the existing listeners
    */
   private final List<ConnectionEventListener> listeners;

   ManagedSessionImpl(SessionImpl session)
   {
      this.session = session;
      this.listeners = new CopyOnWriteArrayList<ConnectionEventListener>();
      session.registerLifecycleListener(new SessionLifecycleListener()
      {
         public void onCloseSession(ExtendedSession session)
         {
            onConnectionClosed();
         }         
      });
   }

   /**
    * @see javax.resource.spi.ManagedConnection#addConnectionEventListener(javax.resource.spi.ConnectionEventListener)
    */
   public void addConnectionEventListener(ConnectionEventListener listener)
   {
      listeners.add(listener);
   }

   /**
    * @see javax.resource.spi.ManagedConnection#associateConnection(java.lang.Object)
    */
   public void associateConnection(Object connection) throws ResourceException
   {
      throw new NotSupportedException("The JCR sessions are not shareable");
   }

   /**
    * @see javax.resource.spi.ManagedConnection#cleanup()
    */
   public void cleanup() throws ResourceException
   {
   }

   /**
    * @see javax.resource.spi.ManagedConnection#destroy()
    */
   public void destroy() throws ResourceException
   {
      session.logout();
   }

   /**
    * {@inheritDoc}
    */
   public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException
   {
      return session;
   }

   /**
    * @see javax.resource.spi.ManagedConnection#getLocalTransaction()
    */
   public LocalTransaction getLocalTransaction() throws ResourceException
   {
      throw new NotSupportedException("Local transactions are not supported");
   }

   /**
    * @see javax.resource.spi.ManagedConnection#getLogWriter()
    */
   public PrintWriter getLogWriter() throws ResourceException
   {
      return null;
   }

   /**
    * @see javax.resource.spi.ManagedConnection#getMetaData()
    */
   public ManagedConnectionMetaData getMetaData() throws ResourceException
   {
      return new ManagedConnectionMetaData()
      {
         public String getUserName() throws ResourceException
         {
            return session.getUserID();
         }
         
         public int getMaxConnections() throws ResourceException
         {
            return 0;
         }
         
         public String getEISProductVersion() throws ResourceException
         {
            return "1.14";
         }
         
         public String getEISProductName() throws ResourceException
         {
            return "eXo JCR";
         }
      };
   }

   /**
    * @see javax.resource.spi.ManagedConnection#getXAResource()
    */
   public XAResource getXAResource() throws ResourceException
   {
      return session.getXAResource();
   }

   /**
    * @see javax.resource.spi.ManagedConnection#removeConnectionEventListener(javax.resource.spi.ConnectionEventListener)
    */
   public void removeConnectionEventListener(ConnectionEventListener listener)
   {
      listeners.remove(listener);
   }

   /**
    * @see javax.resource.spi.ManagedConnection#setLogWriter(java.io.PrintWriter)
    */
   public void setLogWriter(PrintWriter out) throws ResourceException
   {
   }

   /**
    * Broadcasts the connection closed event
    */
   private void onConnectionClosed()
   {
      ConnectionEvent evt = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
      for (ConnectionEventListener listener : listeners)
      {
         try
         {
            listener.connectionClosed(evt);
         }
         catch (Exception e1)
         {
            LOG.warn("An error occurs while notifying the listener " + listener, e1);
         }
      }
   }
}
