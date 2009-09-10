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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.jcr.core.XASession;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.transaction.ExoResource;
import org.exoplatform.services.transaction.TransactionException;
import org.exoplatform.services.transaction.TransactionService;

import javax.jcr.RepositoryException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: XASessionImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class XASessionImpl extends SessionImpl implements XASession, XAResource, ExoResource
{

   /**
    * Session logger.
    */
   private static final Log LOG = ExoLogger.getLogger("jcr.XASessionImpl");

   /**
    * Transaction service.
    */
   private final TransactionService tService;

   /**
    * Transaction resources manager.
    */
   private final TransactionableResourceManager txResourceManager;

   /**
    * Start flags.
    */
   private int startFlags = TMNOFLAGS;

   /**
    * Transaction timeout.
    */
   private int txTimeout;

   /**
    * An arbitrary payload required by the ExoResource implementation.
    */
   private Object payload;

   /**
    * XASessionImpl constructor.
    * 
    * @param workspaceName
    *          workspace name
    * @param userState
    *          user ConversationState
    * @param container
    *          ExoContainer
    * @param tService
    *          Transaction service
    * @param txResourceManager
    *          Transaction resources manager.
    * @throws RepositoryException
    *           Repository error
    */
   XASessionImpl(String workspaceName, ConversationState userState, ExoContainer container,
      TransactionService tService, TransactionableResourceManager txResourceManager) throws RepositoryException
   {
      super(workspaceName, userState, container);
      this.txTimeout = tService.getDefaultTimeout();
      this.tService = tService;
      this.txResourceManager = txResourceManager;
      this.txResourceManager.add(this);
   }

   /**
    * {@inheritDoc}
    */
   public XAResource getXAResource()
   {
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public void delistResource() throws XAException
   {
      try
      {
         if (LOG.isDebugEnabled())
            LOG.debug("Delist session: " + getSessionInfo() + ", " + this);
         tService.delistResource(this);
      }
      catch (RollbackException e)
      {
         throw new XAException(e.getMessage());
      }
      catch (SystemException e)
      {
         throw new XAException(e.getMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void enlistResource() throws XAException
   {
      try
      {
         if (LOG.isDebugEnabled())
            LOG.debug("Enlist session: " + getSessionInfo() + ", " + this);
         tService.enlistResource(this);
      }
      catch (RollbackException e)
      {
         throw new XAException(e.getMessage());
      }
      catch (SystemException e)
      {
         throw new XAException(e.getMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void commit(Xid xid, boolean onePhase) throws XAException
   {
      try
      {
         txResourceManager.commit(this);
      }
      catch (TransactionException e)
      {
         throw new XAException(XAException.XA_RBOTHER);
      }

      if (LOG.isDebugEnabled())
         LOG.debug("Commit. Xid:" + xid + ", session: " + getSessionInfo() + ", " + this);
   }

   /**
    * {@inheritDoc}
    */
   public void end(Xid xid, int flags) throws XAException
   {
      if (LOG.isDebugEnabled())
         LOG.debug("End. Xid:" + xid + ", " + flags + ", session: " + getSessionInfo() + ", " + this);
      startFlags = flags;
   }

   /**
    * {@inheritDoc}
    */
   public void forget(Xid xid) throws XAException
   {
   }

   /**
    * {@inheritDoc}
    */
   public int getTransactionTimeout() throws XAException
   {
      return txTimeout;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSameRM(XAResource resource) throws XAException
   {
      if (resource instanceof XASessionImpl)
      {
         XASessionImpl session = (XASessionImpl)resource;
         boolean isSame = getUserID().equals(session.getUserID());
         if (LOG.isDebugEnabled())
            LOG.debug("isSameRM: " + getSessionInfo() + " -- " + session.getSessionInfo() + " : " + isSame + ", "
               + this + " -- " + session + ", Flags:" + startFlags);
         return isSame;
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public int prepare(Xid xid) throws XAException
   {
      if (LOG.isDebugEnabled())
         LOG.debug("Prepare. Xid:" + xid + ", session: " + getSessionInfo() + ", " + this);
      return XA_OK;
   }

   /**
    * {@inheritDoc}
    */
   public Xid[] recover(int xid) throws XAException
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void rollback(Xid xid) throws XAException
   {
      txResourceManager.rollback(this);
      if (LOG.isDebugEnabled())
         LOG.debug("Rollback. Xid:" + xid + ", session: " + getSessionInfo() + ", " + this);
   }

   /**
    * {@inheritDoc}
    */
   public boolean setTransactionTimeout(int seconds) throws XAException
   {
      try
      {
         tService.setTransactionTimeout(seconds);
      }
      catch (SystemException e)
      {
         throw new XAException(e.getMessage());
      }
      this.txTimeout = seconds;
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void start(Xid xid, int flags) throws XAException
   {
      txResourceManager.start(this);
      startFlags = flags;
      if (LOG.isDebugEnabled())
         LOG.debug("Start. Xid:" + xid + ", " + flags + ", session: " + getSessionInfo() + ", " + this);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void logout()
   {
      if (LOG.isDebugEnabled())
         LOG.debug("Logout. Session: " + getSessionInfo() + ", " + this);

      // Rolling back this session only
      getTransientNodesManager().getTransactManager().rollback();

      // Remove session from this user sessions list
      txResourceManager.remove(this);

      super.logout();
      try
      {
         delistResource();
         startFlags = TMNOFLAGS;
      }
      catch (XAException e)
      {
         LOG.error("Logut error " + e, e);
      }
   }

   /**
    * Get XASession info string.
    * 
    * @return info string
    */
   private String getSessionInfo()
   {
      return getUserID() + "@" + workspaceName;
   }

   public Object getPayload()
   {
      return payload;
   }

   public void setPayload(Object payload)
   {
      this.payload = payload;
   }
}
