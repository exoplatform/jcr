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

import org.exoplatform.services.jcr.impl.core.XASessionImpl;
import org.exoplatform.services.transaction.TransactionException;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.transaction.xa.XAException;

/**
 * Created by The eXo Platform SAS.
 * <p/>
 * Manager provides consistency of transaction operations performed by same user but in different
 * Repository Sessions.
 * <p/>
 * Manager stores list of XASessions involved in transaction by a user and then can be used to
 * broadcast transaction start/commit/rollback to all live Sessions of the user.
 * <p/>
 * Broadcast of operations it's an atomic operation regarding to the Sessions list. Until operation
 * broadcast request is active other requests or list modifications will wait for.
 * <p/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TransactionableResourceManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TransactionableResourceManager
{

   /**
    * XASessions involved in transaction. Sessions stored by userId.
    */
   private ConcurrentHashMap<String, ConcurrentLinkedQueue<SoftReference<XASessionImpl>>> txResources =
      new ConcurrentHashMap<String, ConcurrentLinkedQueue<SoftReference<XASessionImpl>>>();

   /**
    * TransactionableResourceManager constructor.
    */
   public TransactionableResourceManager()
   {
   }

   /**
    * Add session to the transaction group.
    * 
    * @param userSession
    *          XASessionImpl, user XASession
    */
   public void add(XASessionImpl userSession)
   {
      final ConcurrentLinkedQueue<SoftReference<XASessionImpl>> joinedList = txResources.get(userSession.getUserID());
      if (joinedList != null)
      {
         // remove unused session from user list and put this list at the end
         // threads of same user
         for (Iterator<SoftReference<XASessionImpl>> siter = joinedList.iterator(); siter.hasNext();)
         {
            XASessionImpl xaSession = siter.next().get();
            if (xaSession == null || !xaSession.isLive())
            {
               siter.remove();
            }
         }

         joinedList.add(new SoftReference<XASessionImpl>(userSession));

         // make sure the list is not removed by another Session of same user, see
         // remove()
         putIfAbsent(userSession.getUserID(), joinedList);
      }
      else
      {
         // sync for same userId operations
         final ConcurrentLinkedQueue<SoftReference<XASessionImpl>> newJoinedList =
            new ConcurrentLinkedQueue<SoftReference<XASessionImpl>>();
         final ConcurrentLinkedQueue<SoftReference<XASessionImpl>> previous =
            putIfAbsent(userSession.getUserID(), newJoinedList);
         if (previous != null)
         {
            previous.add(new SoftReference<XASessionImpl>(userSession));
         }
         else
         {
            newJoinedList.add(new SoftReference<XASessionImpl>(userSession));
         }
      }
   }

   /**
    * Remove session from user Sessions list.
    * 
    * @param userSession
    *          XASessionImpl, user XASession
    */
   public void remove(XASessionImpl userSession)
   {
      final ConcurrentLinkedQueue<SoftReference<XASessionImpl>> joinedList = txResources.get(userSession.getUserID());
      if (joinedList != null)
      {
         // traverse and remove unused sessions and given one
         // threads of same user
         for (Iterator<SoftReference<XASessionImpl>> siter = joinedList.iterator(); siter.hasNext();)
         {
            XASessionImpl xaSession = siter.next().get();
            if (xaSession == null || !xaSession.isLive() || xaSession == userSession)
            {
               siter.remove();
            }
         }

         // if list is empty - remove mapping to the list
         if (joinedList.size() <= 0)
         {
            txResources.remove(userSession.getUserID());
         }
      }
   }

   /**
    * Commit all sessions.
    * 
    * @param userSession
    *          XASessionImpl, commit initializing session
    * @throws TransactionException
    *           Transaction error
    */
   public void commit(XASessionImpl userSession, boolean onePhase) throws TransactionException
   {
      ConcurrentLinkedQueue<SoftReference<XASessionImpl>> joinedList;
      if (onePhase)
      {
         joinedList = txResources.remove(userSession.getUserID());
      }
      else
      {
         // TODO keep session on commit to be able to rollback all via XASessions(XAResource) and TM lifecycle
         joinedList = txResources.get(userSession.getUserID());
      }

      if (joinedList != null)
      {
         Iterator<SoftReference<XASessionImpl>> it = joinedList.iterator();
         boolean hasCommitedXASession = false;
         try
         {
            while (it.hasNext())
            {
               SoftReference<XASessionImpl> sr = it.next();
               XASessionImpl xaSession = sr.get();
               if (xaSession != null && xaSession.isLive())
               {
                  xaSession.getTransientNodesManager().getTransactManager().commit();
               }
               hasCommitedXASession = true;
            }
         }
         catch (TransactionException e)
         {
            if (onePhase)
            {
               // rollback now
               while (it.hasNext())
               {
                  SoftReference<XASessionImpl> sr = it.next();
                  XASessionImpl xaSession = sr.get();
                  if (xaSession != null && xaSession.isLive())
                  {
                     xaSession.getTransientNodesManager().getTransactManager().rollback();
                  }
               }

               if (hasCommitedXASession)
               {
                  // somethings were commited already
                  throw new TransactionException(XAException.XA_HEURMIX, e);
               }
               else
               {
                  // all things were rolled back
                  throw new TransactionException(XAException.XA_HEURRB, e);
               }
            }

            throw new TransactionException(XAException.XA_RBOTHER, e);
         }
      }
   }

   /**
    * Start transaction on all sessions.
    * 
    * @param userSession
    *          XASessionImpl, start initializing session
    */
   public void start(XASessionImpl userSession)
   {
      ConcurrentLinkedQueue<SoftReference<XASessionImpl>> joinedList = txResources.get(userSession.getUserID());
      if (joinedList != null)
      {
         for (SoftReference<XASessionImpl> sr : joinedList)
         {
            XASessionImpl xaSession = sr.get();
            if (xaSession != null && xaSession.isLive())
            {
               TransactionableDataManager txManager = xaSession.getTransientNodesManager().getTransactManager();
               txManager.start();
            }
         }
      }
   }

   /**
    * Rollback transaction on all sessions.
    * 
    * @param userSession
    *          XASessionImpl, rollback initializing session
    */
   public void rollback(XASessionImpl userSession)
   {
      ConcurrentLinkedQueue<SoftReference<XASessionImpl>> joinedList = txResources.remove(userSession.getUserID());
      if (joinedList != null)
      {
         for (SoftReference<XASessionImpl> sr : joinedList)
         {
            XASessionImpl xaSession = sr.get();
            if (xaSession != null && xaSession.isLive())
            {
               TransactionableDataManager txManager = xaSession.getTransientNodesManager().getTransactManager();
               txManager.rollback();
            }
         }
      }
   }

   private ConcurrentLinkedQueue<SoftReference<XASessionImpl>> putIfAbsent(String key,
      ConcurrentLinkedQueue<SoftReference<XASessionImpl>> value)
   {
      if (!txResources.containsKey(key))
      {
         return txResources.put(key, value);
      }
      else
      {
         return txResources.get(key);
      }
   }

}
