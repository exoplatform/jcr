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

import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS<br>
 *
 * This interface encapsulates methods for the management of locks by session.
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SessionLockManager.java 111 2008-11-11 11:11:11Z serg $
 * @LevelAPI Unsupported
 */
public interface SessionLockManager extends SessionLifecycleListener
{

   /**
    * Invoked by a session to inform that a lock token has been added.
    * 
    * @param lt
    *          added lock token
    */
   void addLockToken(String lt);

   /**
    * Add lock for node.
    * 
    * @param node - NodeImpl
    * @param isDeep - lock is deep
    * @param isSessionScoped - lock is session scoped
    * @param timeOut - lock live time
    * @return Lock
    * @throws LockException
    * @throws RepositoryException
    */
   Lock addLock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeOut) throws LockException,
      RepositoryException;

   /**
    * Returns the Lock object that applies to a node. This may be either a lock on this node itself
    * or a deep lock on a node above this node.
    * 
    * @param node
    *          node
    * @return lock object
    * @throws LockException
    *           if this node is not locked
    * @see javax.jcr.Node#getLock
    */
   LockImpl getLock(NodeImpl node) throws LockException, RepositoryException;

   /**
    * Return lock tokens enshrined by session
    * @return array of lock tokens.
    */
   String[] getLockTokens();

   /**
    * Returns <code>true</code> if the node given holds a lock; otherwise returns <code>false</code>.
    * 
    * @param node
    *          node
    * @return <code>true</code> if the node given holds a lock; otherwise returns <code>false</code>
    * @see javax.jcr.Node#holdsLock
    */
   boolean holdsLock(NodeData node) throws RepositoryException;

   /**
    * Returns <code>true</code> if this node is locked either as a result of a lock held by this node
    * or by a deep lock on a node above this node; otherwise returns <code>false</code>
    * 
    * @param node
    *          node
    * @return <code>true</code> if this node is locked either as a result of a lock held by this node
    *         or by a deep lock on a node above this node; otherwise returns <code>false</code>
    * @throws LockException 
    * @see javax.jcr.Node#isLocked
    */
   boolean isLocked(NodeData node) throws LockException;

   /**
    * Returns <code>true</code> if the specified session holds a lock on the given node; otherwise
    * returns <code>false</code>. <br> Note that <code>isLockHolder(session, node)==true</code>
    * implies <code>holdsLock(node)==true</code>.
    *
    * @param nodeImpl
    *          node implementation
    * @return if the specified session holds a lock on the given node; otherwise returns
    *         <code>false</code>
    */
   boolean isLockHolder(NodeImpl nodeImpl) throws RepositoryException;

   /**
    * Invoked by a session to inform that a lock token has been removed.
    *
    * @param lt
    *          removed lock token
    */
   void removeLockToken(String lt);

   /**
    * Checks this node, is it accessible according to possible locks. If node is locked and current 
    * session is not lockHolder <code> false</code> will be returned. 
    *  
    * @param data - node that must be checked
    * @return true - if lock not exist or current session is LockOwner; false - in other case;
    * @throws LockException - if lock engine exception happens
    */
   boolean checkLocking(NodeData data) throws LockException;
}
