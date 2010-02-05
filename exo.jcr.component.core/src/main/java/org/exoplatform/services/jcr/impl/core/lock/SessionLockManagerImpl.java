/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SessionLockManagerImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class SessionLockManagerImpl implements SessionLockManager
{

   private final String sessionId;

   private final LockManagerImpl lockManager;

   /**
    * Constructor
    */
   public SessionLockManagerImpl(String sessionId, LockManagerImpl lockManager)
   {
      this.sessionId = sessionId;
      this.lockManager = lockManager;
   }

   /**
    * {@inheritDoc}
    */
   public Lock addLock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeOut) throws LockException,
      RepositoryException
   {
      return lockManager.addPendingLock(node, isDeep, isSessionScoped, timeOut);
   }

   /**
    * {@inheritDoc}
    */
   public void addLockToken(String lt)
   {
      lockManager.addLockToken(sessionId, lt);
   }

   /**
    * {@inheritDoc}
    */
   public LockImpl getLock(NodeImpl node) throws LockException, RepositoryException
   {
      return lockManager.getLock(node);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getLockTokens()
   {
      return lockManager.getLockTokens(sessionId);
   }

   /**
    * {@inheritDoc}
    */
   public boolean holdsLock(NodeData node) throws RepositoryException
   {
      return lockManager.holdsLock(node);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLockHolder(NodeImpl node) throws RepositoryException
   {
      return lockManager.isLockHolder(node);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLocked(NodeData node) throws LockException
   {
      return lockManager.isLocked(node);
   }

   /**
    * {@inheritDoc}
    */
   public void removeLockToken(String lt)
   {
      lockManager.removeLockToken(sessionId, lt);
   }

   /**
    * {@inheritDoc}
    */
   public void onCloseSession(ExtendedSession session)
   {
      lockManager.onCloseSession(session);
   }

}
