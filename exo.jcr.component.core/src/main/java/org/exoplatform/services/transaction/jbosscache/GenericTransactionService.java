/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.transaction.jbosscache;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.transaction.ExoResource;
import org.exoplatform.services.transaction.TransactionService;
import org.jboss.cache.transaction.TransactionManagerLookup;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author <a href="mailto:dmitry.kataev@exoplatform.com">Dmytro Katayev</a>
 * @version $Id: GenericTransactionService.java -1   $
 */
public class GenericTransactionService implements TransactionService
{

   /**
    * The default value of a transaction timeout in seconds
    */
   private static final int DEFAULT_TIME_OUT = 60;

   /**
    * TransactionManagerLookup.
    */
   protected final TransactionManagerLookup tmLookup;

   /**
    * The default timeout
    */
   protected final int defaultTimeout;

   /**
    * Indicates if the timeout has to be enforced
    */
   protected final boolean forceTimeout;

   /**
    * The current Transaction Manager
    */
   private volatile TransactionManager tm;

   /**
    * JBossTransactionManagerLookup  constructor.
    *
    * @param tmLookup TransactionManagerLookup
    */
   public GenericTransactionService(TransactionManagerLookup tmLookup)
   {
      this(tmLookup, null);
   }

   public GenericTransactionService(TransactionManagerLookup tmLookup, InitParams params)
   {
      this.tmLookup = tmLookup;
      if (params != null && params.getValueParam("timeout") != null)
      {
         this.defaultTimeout = Integer.parseInt(params.getValueParam("timeout").getValue());
         this.forceTimeout = true;
      }
      else
      {
         this.defaultTimeout = DEFAULT_TIME_OUT;
         this.forceTimeout = false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Xid createXid()
   {
      throw new UnsupportedOperationException("Method createXid() not supported");
   }

   /**
    * {@inheritDoc}
    */
   public void delistResource(ExoResource exores) throws RollbackException, SystemException
   {
      TransactionManager tm = getTransactionManager();
      Transaction tx = tm.getTransaction();
      if (tx != null)
      {
         tx.delistResource(exores.getXAResource(), XAResource.TMNOFLAGS);
      }
      else
      {
         delistResourceOnTxMissing(tm, exores);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void enlistResource(ExoResource exores) throws RollbackException, SystemException
   {
      TransactionManager tm = getTransactionManager();
      Transaction tx = tm.getTransaction();
      if (tx != null)
      {
         tx.enlistResource(exores.getXAResource());
      }
      else
      {
         enlistResourceOnTxMissing(tm, exores);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getDefaultTimeout()
   {
      return defaultTimeout;
   }

   /**
    * {@inheritDoc}
    */
   public TransactionManager getTransactionManager()
   {
      if (tm == null)
      {
         synchronized (this)
         {
            if (tm == null)
            {
               TransactionManager tm;
               try
               {
                  tm = tmLookup.getTransactionManager();
               }
               catch (Exception e)
               {
                  throw new RuntimeException("Transaction manager not found", e);
               }
               if (forceTimeout)
               {
                  // Only set the timeout when a timeout has been given into the
                  // configuration otherwise we assume that the value will be
                  // set at the AS level
                  tm = new TransactionManagerTxTimeoutAware(tm, defaultTimeout);
               }
               this.tm = tm;
            }
         }
      }
      return tm;
   }

   /**
    * {@inheritDoc}
    */
   public UserTransaction getUserTransaction()
   {
      throw new UnsupportedOperationException("Method UserTransaction() not supported");
   }

   /**
    * {@inheritDoc}
    */
   public void setTransactionTimeout(int seconds) throws SystemException
   {
      TransactionManager tm = getTransactionManager();
      tm.setTransactionTimeout(seconds);
   }

   /**
    * Allows to execute an action when we try to enlist a resource when there is no active 
    * transaction
    */
   protected void enlistResourceOnTxMissing(TransactionManager tm, ExoResource exores) throws RollbackException,
      SystemException
   {
   }

   /**
    * Allows to execute an action when we try to delist a resource when there is no active 
    * transaction
    */
   protected void delistResourceOnTxMissing(TransactionManager tm, ExoResource exores) throws RollbackException,
      SystemException
   {
   }

   /**
    * This class is used to enforce the {@link Transaction} timeout when a new transaction is created through the nested {@link TransactionManager}
    * 
    * Created by The eXo Platform SAS
    * Author : Nicolas Filotto 
    *          nicolas.filotto@exoplatform.com
    * 1 févr. 2010
    */
   private static class TransactionManagerTxTimeoutAware implements TransactionManager
   {
      /**
       * The nested {@link TransactionManager}
       */
      private final TransactionManager tm;

      /**
       * The default timeout of the {@link Transaction}
       */
      private final int defaultTimeout;

      public TransactionManagerTxTimeoutAware(TransactionManager tm, int defaultTimeout)
      {
         this.tm = tm;
         this.defaultTimeout = defaultTimeout;
      }

      /**
       * {@inheritDoc}
       */
      public void begin() throws NotSupportedException, SystemException
      {
         tm.begin();
         // Set the default transaction timeout
         tm.setTransactionTimeout(defaultTimeout);
      }

      /**
       * {@inheritDoc}
       */
      public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
         SecurityException, IllegalStateException, SystemException
      {
         tm.commit();
      }

      /**
       * {@inheritDoc}
       */
      public int getStatus() throws SystemException
      {
         return tm.getStatus();
      }

      /**
       * {@inheritDoc}
       */
      public Transaction getTransaction() throws SystemException
      {
         return tm.getTransaction();
      }

      /**
       * {@inheritDoc}
       */
      public void resume(Transaction tx) throws InvalidTransactionException, IllegalStateException, SystemException
      {
         tm.resume(tx);
      }

      /**
       * {@inheritDoc}
       */
      public void rollback() throws IllegalStateException, SecurityException, SystemException
      {
         tm.rollback();
      }

      /**
       * {@inheritDoc}
       */
      public void setRollbackOnly() throws IllegalStateException, SystemException
      {
         tm.setRollbackOnly();
      }

      /**
       * {@inheritDoc}
       */
      public void setTransactionTimeout(int timeout) throws SystemException
      {
         tm.setTransactionTimeout(timeout);
      }

      /**
       * {@inheritDoc}
       */
      public Transaction suspend() throws SystemException
      {
         return tm.suspend();
      }
   }
}
