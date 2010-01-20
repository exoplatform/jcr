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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.core.lock.ExtendedLock;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: LockImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class LockImpl implements ExtendedLock
{
   private LockData lockData;

   private SessionImpl session;

   // TODO remove this constructor, need refactor lock classes ierarchy
   public LockImpl()
   {
   }

   public LockImpl(SessionImpl session, LockData lockData)
   {
      this.lockData = lockData;
      this.session = session;
   }

   public String getLockOwner()
   {
      return lockData.getOwner();
   }

   public String getLockToken()
   {
      return lockData.getLockToken(session.getId());
   }

   public boolean isLive()
   {
      return lockData.isLive();
   }

   public void refresh() throws LockException, RepositoryException
   {
      if (!isLive())
         throw new LockException("Lock is not live");
      lockData.refresh();
   }

   public Node getNode()
   {
      try
      {
         return (Node)session.getTransientNodesManager().getItemByIdentifier(lockData.getNodeIdentifier(), true);
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
      }
      return null;
   }

   public boolean isDeep()
   {

      return lockData.isDeep();
   }

   public boolean isSessionScoped()
   {
      return lockData.isSessionScoped();
   }

   public long getTimeToDeath()
   {
      return lockData.getTimeToDeath();
   }

   protected void setTimeOut(long timeOut) throws LockException
   {
      lockData.setTimeOut(timeOut);
   }
}
