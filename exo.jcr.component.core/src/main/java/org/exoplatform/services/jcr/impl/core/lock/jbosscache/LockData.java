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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.jcr.impl.Constants;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: LockData.java 787 2009-11-20 11:36:15Z nzamosenchuk $
 */

public class LockData implements Externalizable
{
   /**
    * The time of birth. From this time we start count the time of death. death = birthday+TIME_OUT;
    */
   private long birthday;

   /**
    * If isDeep is true then the lock applies to this node and all its descendant nodes; if false,
    * the lock applies only to this, the holding node.
    */
   private boolean deep;

   /**
    * A lock token is a string that uniquely identifies a particular lock and acts as a “key”
    * allowing a user to alter a locked node. LockData stores only token hash.
    */
   private String tokenHash;

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
   private boolean sessionScoped;

   /**
    * <B>8.4.9 Timing Out</B> An implementation may unlock any lock at any time due to
    * implementation-specific criteria, such as time limits on locks.
    */
   private long timeOut;

   // Need for Externalizable
   public LockData()
   {
      this.sessionScoped = false;
      this.deep = false;
   }

   /**
    * @param nodeIdentifier
    * @param lockToken
    * @param deep
    * @param sessionScoped
    * @param owner
    * @param timeOut
    *       is seconds!
    */
   public LockData(String nodeIdentifier, String lockTokenHash, boolean deep, boolean sessionScoped, String owner,
      long timeOut)
   {
      this(nodeIdentifier, lockTokenHash, deep, sessionScoped, owner, timeOut, System.currentTimeMillis());
   }
   
   /**
    * @param nodeIdentifier
    * @param lockToken
    * @param deep
    * @param sessionScoped
    * @param owner
    * @param timeOut
    *       is seconds!
    * @param birthday
    */
   public LockData(String nodeIdentifier, String lockTokenHash, boolean deep, boolean sessionScoped, String owner,
      long timeOut, long birthday)
   {
      this.nodeIdentifier = nodeIdentifier;
      this.tokenHash = lockTokenHash;
      this.deep = deep;
      this.sessionScoped = sessionScoped;
      this.owner = owner;
      this.timeOut = timeOut;
      this.birthday = birthday;
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

   public String getTokenHash()
   {
      return tokenHash;
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return tokenHash.hashCode();
   }

   public boolean isDeep()
   {
      return deep;
   }

   /**
    * @return
    */
   public boolean isSessionScoped()
   {
      return sessionScoped;
   }

   /**
    * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      // read boolean
      this.deep = in.readBoolean();
      this.sessionScoped = in.readBoolean();
      // read long
      this.birthday = in.readLong();
      this.timeOut = in.readLong();
      //read strings
      // read uuid
      byte[] buf;
      buf = new byte[in.readInt()];
      in.readFully(buf);
      this.nodeIdentifier = new String(buf, Constants.DEFAULT_ENCODING);
      // read owner
      buf = new byte[in.readInt()];
      in.readFully(buf);
      this.owner = new String(buf, Constants.DEFAULT_ENCODING);
      // read token
      buf = new byte[in.readInt()];
      in.readFully(buf);
      this.tokenHash = new String(buf, Constants.DEFAULT_ENCODING);
   }

   /**
    * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // write boolean
      out.writeBoolean(deep);
      out.writeBoolean(sessionScoped);
      // write long
      out.writeLong(birthday);
      out.writeLong(timeOut);
      // write string
      // node uuid
      byte[] ptbuf = nodeIdentifier.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(ptbuf.length);
      out.write(ptbuf);
      // node owner
      ptbuf = owner.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(ptbuf.length);
      out.write(ptbuf);
      // node token
      ptbuf = tokenHash.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(ptbuf.length);
      out.write(ptbuf);

   }

   /**
    * @return
    */
   public long getTimeOut()
   {
      return timeOut;
   }

   public long getBirthDay()
   {
      return birthday;
   }

   public void setTimeOut(long timeOut)
   {
      this.timeOut = timeOut;
   }

}
