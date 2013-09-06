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
package org.exoplatform.services.jcr.impl.dataflow.session;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LocalWorkspaceDataManagerStub;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;

import java.lang.ref.SoftReference;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Created by The eXo Platform SAS.
 * <p/>
 * Used to perform an atomic prepare/commit/rollback between all the JCR sessions tied
 * to the current transaction
 * <p/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TransactionableResourceManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TransactionableResourceManager implements XAResource
{

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.TransactionableResourceManager");

   /**
    * Sessions involved in transaction. 
    */
   private final ThreadLocal<TransactionContext> contexts = new ThreadLocal<TransactionContext>();

   /**
    * Transaction manager.
    */
   private final TransactionManager tm;

   /**
    * The eXo container in which the TransactionableResourceManager has been registered
    */
   private final ExoContainer container;

   /**
    * The data manager
    */
   private volatile LocalWorkspaceDataManagerStub workspaceDataManager;

   /**
    * The current tx timeout in seconds
    */
   private int txTimeout;

   /**
    * TransactionableResourceManager constructor.
    */
   public TransactionableResourceManager(ExoContainerContext ctx)
   {
      this(ctx, null);
   }

   /**
    * TransactionableResourceManager constructor.
    */
   public TransactionableResourceManager(ExoContainerContext ctx, TransactionService tService)
   {
      this.tm = tService == null ? null : tService.getTransactionManager();
      this.container = ctx.getContainer();
   }

   /**
    * Lazily gets the LocalWorkspaceDataManagerStub from the eXo container. This is required to
    * prevent cyclic dependency
    */
   private LocalWorkspaceDataManagerStub getWorkspaceDataManager()
   {
      if (workspaceDataManager == null)
      {
         synchronized (this)
         {
            if (workspaceDataManager == null)
            {
               LocalWorkspaceDataManagerStub workspaceDataManager =
                  (LocalWorkspaceDataManagerStub)container
                     .getComponentInstanceOfType(LocalWorkspaceDataManagerStub.class);
               if (workspaceDataManager == null)
               {
                  throw new IllegalStateException("The workspace data manager cannot be found");
               }
               this.workspaceDataManager = workspaceDataManager;
            }
         }
      }
      return workspaceDataManager;
   }

   /**
    * Indicates whether a global tx is active or not
    * @return <code>true</code> if a global tx is active, <code>false</code> otherwise.
    */
   public boolean isGlobalTxActive()
   {
      TransactionContext ctx;
      try
      {
         // We need to check the status also to be able to manage properly suspend and resume
         return (ctx = contexts.get()) != null && ctx.getXidContext() != null && tm.getStatus() != Status.STATUS_NO_TRANSACTION;
      }
      catch (SystemException e)
      {
         log.warn("Could not check if a global Tx has been started", e);
      }
      return false;
   }

   /**
    * Registers an object to be shared within the XidContext
    * @param key the key of the shared object
    * @param value the shared object
    */
   public void putSharedObject(String key, Object value)
   {
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         throw new IllegalStateException("There is no active transaction context");
      }
      XidContext xidCtx = ctx.getXidContext();
      if (xidCtx == null)
      {
         throw new IllegalStateException("There is no active xid context");
      }
      xidCtx.putSharedObject(key, value);
   }

   /**
    * Gives the shared object corresponding to the given key
    * @param key the key of the shared object
    * @return the corresponding shared object
    */
   public <T> T getSharedObject(String key)
   {
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         throw new IllegalStateException("There is no active transaction context");
      }
      XidContext xidCtx = ctx.getXidContext();
      if (xidCtx == null)
      {
         throw new IllegalStateException("There is no active xid context");
      }
      return xidCtx.getSharedObject(key);
   }

   /**
    * Checks if a global Tx has been started if so the session and its change will be dynamically enrolled
    * @param session the session to enlist in case a Global Tx has been started
    * @param changes the changes to enlist in case a Global Tx has been started 
    * @return <code>true</code> if a global Tx has been started and the session and its change could
    * be enrolled successfully, <code>false</code> otherwise
    * @throws IllegalStateException if the current status of the global transaction is not appropriate
    */
   public boolean canEnrollChangeToGlobalTx(final SessionImpl session, final PlainChangesLog changes)
   {
      try
      {
         int status;
         if (tm != null && (status = tm.getStatus()) != Status.STATUS_NO_TRANSACTION)
         {
            if (status != Status.STATUS_ACTIVE && status != Status.STATUS_PREPARING)
            {
               throw new IllegalStateException("The session cannot be enrolled in the current global transaction due "
                  + "to an invalidate state, the current status is " + status
                  + " and only ACTIVE and PREPARING are allowed");
            }
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
               public Void run() throws Exception
               {
                  add(session, changes);
                  return null;
               }
            });
            return true;
         }
      }
      catch (PrivilegedActionException e)
      {
         log.warn("Could not check if a global Tx has been started or register the session into the resource manager",
            e);
      }
      catch (SystemException e)
      {
         log.warn("Could not check if a global Tx has been started or register the session into the resource manager",
            e);
      }
      return false;
   }

   /**
    * Add a new listener to register to the current tx
    * @param listener the listener to add
    */
   public void addListener(TransactionableResourceManagerListener listener)
   {
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         throw new IllegalStateException("There is no active transaction context");
      }
      XidContext xidCtx = ctx.getXidContext();
      if (xidCtx == null)
      {
         throw new IllegalStateException("There is no active xid context");
      }
      xidCtx.addListener(listener);
   }

   /**
    * Add session to the transaction group.
    * 
    * @param userSession
    *          SessionImpl, user Session
    * @throws SystemException 
    * @throws RollbackException 
    * @throws IllegalStateException 
    */
   private void add(SessionImpl session, PlainChangesLog changes) throws SystemException, IllegalStateException,
      RollbackException
   {
      Transaction tx = tm.getTransaction();
      if (tx == null)
      {
         // No active tx so there is no need to register the session
         return;
      }
      // Get the current TransactionContext
      TransactionContext ctx = getOrCreateTransactionContext();
      // Register the tx if it has not been done already
      ctx.registerTransaction(tx);
      // Register the given changes
      ctx.add(session, changes);
   }

   /**
    * Gives the current {@link TransactionContext} from the ThreadLocal and create it doesn't exist
    */
   private TransactionContext getOrCreateTransactionContext()
   {
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         // No transaction context exists so we create a new one
         ctx = new TransactionContext();
         contexts.set(ctx);
      }
      return ctx;
   }

   /**
    * This synchronization is used to apply all changes before commit phase and it is also used
    * to execute actions once the tx is completed which is necessary in case we use non tx aware
    * resources like the lucene indexes and the observation
    * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
    * @version $Id$
    *
    */
   private class TransactionableResourceManagerSynchronization implements Synchronization
   {
      private final Xid xid;

      /**
       * Indicates whether or not there is another after completion method to call
       */
      private final AtomicBoolean isLastAfterCompletion = new AtomicBoolean(true);

      public TransactionableResourceManagerSynchronization(Xid xid)
      {
         this.xid = xid;
      }

      /**
       * @see javax.transaction.Synchronization#beforeCompletion()
       */
      public void beforeCompletion()
      {
         if (log.isDebugEnabled())
         {
            log.debug("BeforeCompletion Xid:" + xid + ": " + this);
         }
         TransactionContext ctx = contexts.get();
         if (ctx == null)
         {
            if (log.isDebugEnabled())
            {
               log.debug("Could not find the context");
            }
            return;
         }
         else
         {
            // define the current branch id
            ctx.setXid(xid);
         }
         Map<PlainChangesLog, SessionImpl> changes = ctx.getChanges(xid);
         if (changes == null || changes.isEmpty())
         {
            if (log.isDebugEnabled())
            {
               log.debug("There is no change to apply");
            }
            return;
         }
         try
         {
            TransactionChangesLog allChanges = new TransactionChangesLog();
            for (Map.Entry<PlainChangesLog, SessionImpl> entry : changes.entrySet())
            {
               SessionImpl session = entry.getValue();
               // first check if the tx was not too long
               if (session.hasExpired())
               {
                  // at least one session has expired so we abort the tx
                  throw new RepositoryException("The tx was too long, at least one session has expired.");
               }
               // Add the change following the chronology order
               allChanges.addLog(entry.getKey());
            }
            getWorkspaceDataManager().save(allChanges);
         }
         catch (RepositoryException e)
         {
            log.error("Could not apply changes", e);
            setRollbackOnly();
            return;
         }
         // Since between 2 TM implementations the afterCompletion can be called in the same 
         // order as the synchronization order or in the reverse order, so we add a second synchronization
         // such that the afterCompletion method will be called in the second call
         try
         {
            tm.getTransaction().registerSynchronization(new Synchronization()
            {

               public void beforeCompletion()
               {
               }

               public void afterCompletion(int status)
               {
                  TransactionableResourceManagerSynchronization.this.afterCompletion(status);
               }
            });
            // Indicates that there is at least one after completion method to come
            isLastAfterCompletion.set(false);
         }
         catch (RollbackException e)
         {
            log.error("Could not register the second synchronization", e);
         }
         catch (IllegalStateException e)
         {
            log.error("Could not register the second synchronization", e);
         }
         catch (SystemException e)
         {
            log.error("Could not register the second synchronization", e);
         }
      }

      /**
       * @see javax.transaction.Synchronization#afterCompletion(int)
       */
      public void afterCompletion(int status)
      {
         if (log.isDebugEnabled())
         {
            log.debug("AfterCompletion Xid:" + xid + ", " + status + ", " + isLastAfterCompletion.get() + ": " + this);
         }
         if (!isLastAfterCompletion.get())
         {
            isLastAfterCompletion.set(true);
            return;
         }
         TransactionContext ctx = contexts.get();
         if (ctx == null)
         {
            if (log.isDebugEnabled())
            {
               log.debug("Could not find the context");
            }
            return;
         }
         XidContext xidCtx = ctx.getXidContext(xid);
         if (xidCtx == null)
         {
            if (log.isDebugEnabled())
            {
               log.debug("Could not find the xid context");
            }
            return;
         }
         try
         {
            List<TransactionableResourceManagerListener> listeners = xidCtx.getListeners();
            for (int i = 0, length = listeners.size(); i < length; i++)
            {
               TransactionableResourceManagerListener listener = listeners.get(i);
               try
               {
                  listener.onAfterCompletion(status);
               }
               catch (Exception e)
               {
                  log.error("Could not execute the method onAfterCompletion for the status " + status, e);
               }
            }

            Map<PlainChangesLog, SessionImpl> changes = xidCtx.getMapChanges();
            if (changes != null && !changes.isEmpty())
            {
               for (Map.Entry<PlainChangesLog, SessionImpl> entry : changes.entrySet())
               {
                  SessionImpl session = entry.getValue();
                  TransactionableDataManager txManager = session.getTransientNodesManager().getTransactManager();
                  // Remove the change from the tx change log. Please note that a simple reset cannot
                  // be done since the session could be enrolled in several tx, so each change need to
                  // be scoped to a given xid
                  txManager.removeLog(entry.getKey());
               }
            }
         }
         finally
         {
            ctx.remove(xid);
         }
      }
   }

   /**
    * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
    */
   public void commit(Xid xid, boolean onePhase) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Commit. Xid:" + xid + ", onePhase: " + onePhase + ": " + this);
      }
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Could not find the context");
         }
         return;
      }
      XidContext xidCtx = ctx.getXidContext(xid);
      if (xidCtx != null)
      {
         boolean failed = false;
         List<TransactionableResourceManagerListener> listeners = xidCtx.getListeners();
         for (int i = 0, length = listeners.size(); i < length; i++)
         {
            TransactionableResourceManagerListener listener = listeners.get(i);
            try
            {
               listener.onCommit(onePhase);
            }
            catch (Exception e)
            {
               log.error("Could not execute the method onCommit(" + onePhase + ")", e);
               failed = true;
               break;
            }
         }
         if (failed)
         {
            if (onePhase)
            {
               // In case of one phase commit, we are supposed to roll back the branch
               abort(listeners);
               throw new XAException(XAException.XA_RBROLLBACK);
            }
            throw new XAException(XAException.XAER_RMERR);
         }
      }
   }

   /**
    * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
    */
   public void end(Xid xid, int flags) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("End. Xid:" + xid + ", " + flags + ": " + this);
      }
   }

   /**
    * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
    */
   public void forget(Xid xid) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Forget. Xid:" + xid + ": " + this);
      }
      abort(xid);
   }

   /**
    * @see javax.transaction.xa.XAResource#getTransactionTimeout()
    */
   public int getTransactionTimeout() throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("GetTransactionTimeout. : " + this);
      }
      return txTimeout;
   }

   /**
    * @return the transaction timeout in milliseconds
    */
   private long getTransactionTimeoutMillis()
   {
      return txTimeout * 1000L;
   }

   /**
    * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
    */
   public boolean isSameRM(XAResource xares) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("IsSameRM. XAResource:" + xares + ": " + this);
      }

      // We have one XAResource per workspace
      return xares == this;
   }

   /**
    * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
    */
   public int prepare(Xid xid) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Prepare. Xid:" + xid + ": " + this);
      }

      return XA_OK;
   }

   /**
    * @see javax.transaction.xa.XAResource#recover(int)
    */
   public Xid[] recover(int flag) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Recover. flag:" + flag + ": " + this);
      }
      return new Xid[0];
   }

   /**
    * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
    */
   public void rollback(Xid xid) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Rollback. Xid:" + xid + ": " + this);
      }
      abort(xid);
   }

   /**
    * @param xid
    */
   private void abort(Xid xid) throws XAException
   {
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Could not find the context");
         }
         return;
      }
      XidContext xidCtx = ctx.getXidContext(xid);
      if (xidCtx != null)
      {
         List<TransactionableResourceManagerListener> listeners = xidCtx.getListeners();
         abort(listeners);
      }
   }

   /**
    * Call the method onAbort on all the given listeners
    * 
    * @param listeners all the listeners on which we need to call the onAbort method
    * @throws XAException if an error occurs while calling one of the onAbort methods
    */
   private void abort(List<TransactionableResourceManagerListener> listeners) throws XAException
   {
      boolean exception = false;
      for (int i = 0, length = listeners.size(); i < length; i++)
      {
         TransactionableResourceManagerListener listener = listeners.get(i);
         try
         {
            listener.onAbort();
         }
         catch (Exception e)
         {
            log.error("Could not execute the method onAbort", e);
            exception = true;
         }
      }
      if (exception)
      {
         throw new XAException(XAException.XAER_RMERR);
      }
   }

   /**
    * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
    */
   public boolean setTransactionTimeout(int txTimeout) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("SetTransactionTimeout. " + txTimeout + ": " + this);
      }
      this.txTimeout = txTimeout;
      // Set the new timeout on the current sessions
      TransactionContext ctx = contexts.get();
      if (ctx == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug("Could not find the context");
         }
         return true;
      }
      Set<SessionImpl> sessions = ctx.getSessions();
      if (sessions != null)
      {
         for (SessionImpl session : sessions)
         {
            session.setTimeout(getTransactionTimeoutMillis());
         }
      }
      return true;
   }

   /**
    * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
    */
   public void start(Xid xid, int flags) throws XAException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Start. Xid:" + xid + ", " + flags + ": " + this);
      }
      // Get the current TransactionContext
      TransactionContext ctx = getOrCreateTransactionContext();
      // define the current branch id
      ctx.setXid(xid);
   }

   /**
    * Change the status of the tx to 'rollback-only'
    */
   private void setRollbackOnly()
   {
      try
      {
         tm.getTransaction().setRollbackOnly();
      }
      catch (IllegalStateException e)
      {
         log.warn("Could not set the status of the tx to 'rollback-only'", e);
      }
      catch (SystemException e)
      {
         log.warn("Could not set the status of the tx to 'rollback-only'", e);
      }
   }

   private class TransactionContext
   {
      /**
       * The soft reference of the current tx
       */
      private SoftReference<Transaction> srTx;

      /**
       * The soft reference of the current branch id
       */
      private SoftReference<Xid> srXid;

      /**
       * The the current contexts stored by Xid
       */
      private Map<Xid, XidContext> xidContexts;

      /**
       * This method registers the current tx if it is a new one, and if so
       * enlist the resource.
       * @param tx the current tx
       * @throws SystemException
       * @throws IllegalStateException
       * @throws RollbackException
       */
      public void registerTransaction(Transaction tx) throws SystemException, IllegalStateException, RollbackException
      {
         if (!tx.equals(getTransaction()))
         {
            // The current tx has changed
            // We store the new current tx
            setTransaction(tx);
            // We enlist the resource to get the new branch id
            tx.enlistResource(TransactionableResourceManager.this);
         }
      }

      public XidContext getXidContext()
      {
         // Get the current Xid from context
         Xid xid = getXid();
         if (xid == null || xidContexts == null)
         {
            return null;
         }
         return xidContexts.get(xid);
      }

      public XidContext getXidContext(Xid xid)
      {
         if (xid == null || xidContexts == null)
         {
            return null;
         }
         return xidContexts.get(xid);
      }

      public Map<PlainChangesLog, SessionImpl> getChanges(Xid xid)
      {
         XidContext ctx = getXidContext(xid);
         if (ctx == null)
         {
            return null;
         }
         return ctx.getMapChanges();
      }

      private void setTransaction(Transaction tx)
      {
         srTx = new SoftReference<Transaction>(tx);
      }

      public Transaction getTransaction()
      {
         return srTx == null ? null : srTx.get();
      }

      public void setXid(Xid xid)
      {
         srXid = new SoftReference<Xid>(xid);
      }

      private Xid getXid()
      {
         return srXid == null ? null : srXid.get();
      }

      public Set<SessionImpl> getSessions()
      {
         // Get the current Xid from context
         Xid xid = getXid();
         if (xid == null)
         {
            return null;
         }
         return getSessions(xid);
      }

      private Set<SessionImpl> getSessions(Xid xid)
      {
         Map<PlainChangesLog, SessionImpl> changes = getChanges(xid);
         return changes == null ? null : new HashSet<SessionImpl>(changes.values());
      }

      /**
       * Add the given session and changes to the transaction context.
       * @param session the session to add to the transaction context
       * @param changes the session to add to the transaction context
       * @throws IllegalStateException
       * @throws RollbackException
       * @throws SystemException
       */
      public void add(SessionImpl session, PlainChangesLog changes) throws IllegalStateException, RollbackException,
         SystemException
      {
         if (xidContexts == null)
         {
            // No xid context has been defined, so we create a new one
            this.xidContexts = new WeakHashMap<Xid, XidContext>();
         }
         // Get the current Xid from context
         Xid xid = getXid();
         if (xid == null)
         {
            throw new IllegalStateException("Threre is no active branch");
         }
         // Retrieve the corresponding sub-context
         XidContext ctx = xidContexts.get(xid);
         if (ctx == null)
         {
            // No sub-context for this xid, we then create a new context
            ctx = new XidContext();
            xidContexts.put(xid, ctx);
            boolean registered = false;
            try
            {
               // since it is a new branch we can register the corresponding synchronization
               getTransaction().registerSynchronization(new TransactionableResourceManagerSynchronization(xid));
               registered = true;
            }
            finally
            {
               if (!registered)
               {
                  // if an error occurs automatically unregister the branch
                  remove(xid);
               }
            }
         }
         if (txTimeout > 0)
         {
            // Set the timeout
            session.setTimeout(getTransactionTimeoutMillis());
         }
         if (changes != null)
         {
            // Add the changes and the session to the context
            ctx.put(changes, session);
            // Start the transaction mode
            session.getTransientNodesManager().getTransactManager().start();
         }
      }

      public void remove(Xid xid)
      {
         if (xidContexts != null)
         {
            xidContexts.remove(xid);
            if (xidContexts.isEmpty())
            {
               // There is not xid context anymore
               // Remove the context from the TL
               contexts.set(null);
            }
         }
      }
   }

   /**
    * This class encapsulates all the information related to a given Xid
    * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
    * @version $Id$
    *
    */
   private static class XidContext
   {
      /**
       * The list of all the existing listeners
       */
      private final List<TransactionableResourceManagerListener> listeners =
         new ArrayList<TransactionableResourceManagerListener>();

      /**
       * The map of all changes
       */
      private final Map<PlainChangesLog, SessionImpl> mapChanges = new LinkedHashMap<PlainChangesLog, SessionImpl>();

      /**
       * The map containing all the objects that we would like to share within the XidContext
       */
      private final Map<String, Object> sharedObjects = new HashMap<String, Object>();

      /**
       * @return the listeners
       */
      public List<TransactionableResourceManagerListener> getListeners()
      {
         return listeners;
      }

      /**
       * @param listener the listener to add to the list of listeners
       */
      public void addListener(TransactionableResourceManagerListener listener)
      {
         listeners.add(listener);
      }

      /**
       * @return the mapChanges
       */
      public Map<PlainChangesLog, SessionImpl> getMapChanges()
      {
         return mapChanges;
      }

      /**
       * Registers changes for a given session
       * @param changes the changes to add
       * @param session the session related to the changes
       */
      public void put(PlainChangesLog changes, SessionImpl session)
      {
         mapChanges.put(changes, session);
      }

      /**
       * Registers an object to be shared within the XidContext
       * @param key the key of the shared object
       * @param value the shared object
       */
      public void putSharedObject(String key, Object value)
      {
         sharedObjects.put(key, value);
      }

      /**
       * Gives the shared object corresponding to the given key
       * @param key the key of the shared object
       * @return the corresponding shared object
       */
      @SuppressWarnings("unchecked")
      public <T> T getSharedObject(String key)
      {
         return (T)sharedObjects.get(key);
      }
   }
}
