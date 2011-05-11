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

import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.lock.LockImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CacheLockImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class CacheLockImpl extends LockImpl
{
   private boolean live;

   private LockData lockData;

   private SessionImpl session;

   private CacheableSessionLockManager lockManager;

   /**
    * Constructor.
    * 
    * @param session - session owner
    * @param lockData - LockData
    * @param lockManager - CacheableLockManager
    */
   public CacheLockImpl(SessionImpl session, LockData lockData, CacheableSessionLockManager lockManager)
   {
      this.lockData = lockData;
      this.session = session;
      this.lockManager = lockManager;
      this.live = true;
   }

   /**
    * {@inheritDoc}
    */
   public String getLockOwner()
   {
      return lockData.getOwner();
   }

   /**
    * {@inheritDoc}
    */
   public String getLockToken()
   {
      return lockManager.getLockToken(lockData.getTokenHash());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLive() throws LockException
   {
      if (!live)
      {
         // it is already not alive
         return false;
      }
      live = lockManager.isLockLive(lockData.getNodeIdentifier());
      return live;
   }

   /**
    * {@inheritDoc}
    */
   public void refresh() throws LockException, RepositoryException
   {
      if (!isLive())
         throw new LockException("Lock is not live");

      LockData newLockData =
         new LockData(lockData.getNodeIdentifier(), lockData.getTokenHash(), lockData.isDeep(), lockData
            .isSessionScoped(), lockData.getOwner(), lockData.getTimeOut());

      lockManager.refresh(newLockData);
      lockData = newLockData;
   }

   /**
    * {@inheritDoc}
    */
   public Node getNode()
   {
      try
      {
         return (Node)session.getTransientNodesManager().getItemByIdentifier(lockData.getNodeIdentifier(), true);
      }
      catch (RepositoryException e)
      {
         //TODO
         e.printStackTrace();
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDeep()
   {

      return lockData.isDeep();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSessionScoped()
   {
      return lockData.isSessionScoped();
   }

   /**
    * {@inheritDoc}
    */
   public long getTimeToDeath()
   {
      return lockData.getTimeToDeath();
   }

   /**
    * {@inheritDoc}
    */
   protected void setTimeOut(long timeOut) throws LockException
   {
      lockData.setTimeOut(timeOut);

      //reset lock data
      lockManager.refresh(lockData);
   }
}
