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

import java.util.HashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: LockData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class LockData
{
   /**
    * The time of birth. From this time we start count the time of death. death = birthday+TIME_OUT;
    */
   private long birthday;

   /**
    * If isDeep is true then the lock applies to this node and all its descendant nodes; if false,
    * the lock applies only to this, the holding node.
    */
   private final boolean deep;

   /**
    * 
    */
   private boolean live;

   /**
    * List of session id's which holds a lock tokens.
    */
   private final Set<String> lockHolders = new HashSet<String>();

   /**
    * A lock token is a string that uniquely identifies a particular lock and acts as a “key”
    * allowing a user to alter a locked node.
    */
   private String lockToken;

   /**
    * Identifier of locked node.
    */
   private String nodeIdentifier;

   /**
    * The owner of the locked node.
    */
   private String owner;

   /**
    * If isSessionScoped is true then this lock will expire upon the expiration of the current
    * session (either through an automatic or explicit Session.logout); if false, this lock does not
    * expire until explicitly unlocked or automatically unlocked due to a implementation-specific
    * limitation, such as a timeout.
    */
   private final boolean sessionScoped;

   /**
    * <B>8.4.9 Timing Out</B> An implementation may unlock any lock at any time due to
    * implementation-specific criteria, such as time limits on locks.
    */
   private long timeOut;

   /**
    * @param nodeIdentifier
    * @param lockToken
    * @param deep
    * @param sessionScoped
    * @param owner
    * @param timeOut
    */
   public LockData(String nodeIdentifier, String lockToken, boolean deep, boolean sessionScoped, String owner,
      long timeOut)
   {
      this.nodeIdentifier = nodeIdentifier;
      this.lockToken = lockToken;
      this.deep = deep;
      this.sessionScoped = sessionScoped;
      this.owner = owner;
      this.timeOut = timeOut;
      this.live = true;
      this.birthday = System.currentTimeMillis();
   }

   /**
    * @param sessionId
    * @return
    */
   public boolean addLockHolder(String sessionId)
   {
      return lockHolders.add(sessionId);
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (super.equals(obj))
      {
         return true;
      }
      if (obj instanceof LockData)
      {
         return hashCode() == obj.hashCode();
      }
      return false;
   }

   public int getLockHolderSize()
   {
      return lockHolders.size();
   }

   public String getLockToken(String sessionId)
   {
      if (isLockHolder(sessionId))
      {
         return lockToken;
      }
      return null;
   }

   /**
    * @return the nodeIdentifier
    */
   public String getNodeIdentifier()
   {
      return nodeIdentifier;
   }

   /**
    * @return
    */
   public String getOwner()
   {
      return owner;
   }

   /**
    * @return The time to death in millis
    */
   public long getTimeToDeath()
   {
      return birthday + timeOut - System.currentTimeMillis();
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return super.hashCode() + lockToken.hashCode();
   }

   public boolean isDeep()
   {
      return deep;
   }

   /**
    * @return the live
    */
   public boolean isLive()
   {
      return live;
   }

   /**
    * @param sessionId
    * @return
    */
   public boolean isLockHolder(String sessionId)
   {
      return lockHolders.contains(sessionId);
   }

   /**
    * @return
    */
   public boolean isSessionScoped()
   {
      return sessionScoped;
   }

   /**
    * 
    */
   public void refresh()
   {
      birthday = System.currentTimeMillis();
   }

   /**
    * @param sessionId
    * @return
    */
   public boolean removeLockHolder(String sessionId)
   {
      return lockHolders.remove(sessionId);
   }

   /**
    * @param live
    *          live to set
    */
   public void setLive(boolean live)
   {
      this.live = live;
   }

   /**
    * @param lockToken
    */
   public void setLockToken(String lockToken)
   {
      this.lockToken = lockToken;
   }

   /**
    * @param nodeIdentifier
    *          the nodeIdentifier to set
    */
   public void setNodeIdentifier(String nodeIdentifier)
   {
      this.nodeIdentifier = nodeIdentifier;
   }

   /**
    * @param owner
    */
   public void setOwner(String owner)
   {
      this.owner = owner;
   }

   /**
    * @return
    */
   protected long getTimeOut()
   {
      return timeOut;
   }

   /**
    * @param timeOut
    */
   protected void setTimeOut(long timeOut)
   {
      this.timeOut = timeOut;
   }
}
