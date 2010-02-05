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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CacheableLockManager.java 111 2008-11-11 11:11:11Z serg $
 */
public interface CacheableLockManager extends WorkspaceLockManager
{

   // Search constants
   /**
    * The exact lock token.
    */
   public static final int SEARCH_EXECMATCH = 1;

   /**
    * Lock token of closed parent
    */
   public static final int SEARCH_CLOSEDPARENT = 2;

   /**
    * Lock token of closed child
    */
   public static final int SEARCH_CLOSEDCHILD = 4;

   /**
    * Is lock live for node by nodeIdentifier.
    * 
    * @param nodeIdentifier
    * 
    * @return boolean
    * @throws LockException TODO
    */
   boolean isLockLive(String nodeIdentifier) throws LockException;

   /**
    * Search lock in storage. SearchType shows which locks should be returned.
    * See SEARCH_EXECMATCH, SEARCH_CLOSEDPARENT, SEARCH_CLOSEDCHILD. SearchTypes may be combined.
    * 
    * @param node - base node to search locks 
    * @param searchType - combination of SEARCH_EXECMATCH, SEARCH_CLOSEDPARENT, SEARCH_CLOSEDCHILD search types
    * @return LockData or null
    */
   // public LockData getLockData(NodeData node, int searchType) throws LockException;

   /**
    * Replace old lockData with new one. Node ID, token can't be replaced.
    * 
    * @param newLockData
    * @throws LockException
    */
   void refreshLockData(LockData newLockData) throws LockException;

   /**
    * Get default lock timeout.
    * 
    * @return long value of timeout
    */
   long getDefaultLockTimeOut();

   /**
    * Return hash for lock token.
    * 
    * @param lockToken - lock token string 
    * @return - hash string
    */
   String getLockTokenHash(String lockToken);

   LockData getExactNodeLock(NodeData node) throws RepositoryException;

   LockData getExactNodeOrCloseParentLock(NodeData node) throws RepositoryException;

   LockData getClosedChild(NodeData node) throws RepositoryException;
}
