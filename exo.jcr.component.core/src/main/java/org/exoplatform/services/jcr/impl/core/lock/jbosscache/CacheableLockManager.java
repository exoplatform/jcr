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

   /**
    * Returns new LockData object or throw LockException if can't place lock here.
    * 
    * @param node
    * @param lockToken
    * @param isDeep
    * @param isSessionScoped
    * @param timeOut
    * @return LockData object 
    * @throws LockException - if node can not be locked
    */
   LockData createLockData(NodeData node, String lockToken, boolean isDeep, boolean isSessionScoped, String owner,
      long timeOut) throws LockException;

   /**
    * Is lock assigned exactly for node exist.
    * 
    * @param node
    * @return 
    */
   boolean exactLockExist(NodeData node);

   /**
    * Returns lock data that is assigned to this node or its parent.
    * 
    * @param node
    * @return
    */
   LockData getExactOrCloseParentLock(NodeData node);

   String getHash(String lockToken);

   /**
    * Is lock live
    * 
    * @param nodeIdentifier
    * @return
    */
   boolean isLockLive(String nodeIdentifier);

   /**
    * Replace old lockData with new one. Node ID, token can't be replaced.
    * 
    * @param newLockData
    * @throws LockException
    */
   void refreshLockData(LockData newLockData) throws LockException;

}
