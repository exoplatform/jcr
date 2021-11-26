/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.services.jcr.impl.Constants;
import org.infinispan.distribution.group.Group;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS. <br>
 * Base class for WorkspaceCache keys.<br>
 * 
 * Date: 10.06.2008<br>
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: CacheKey.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public abstract class CacheKey implements Externalizable, Comparable<CacheKey>
{

   /**
    * This id will be the unique identifier of the workspace in case the
    * distributed mode is enabled as the cache will be then shared so we
    * need this id to prevent mixing data of different workspace. In case
    * the workspace is not distributed the value of this variable will be
    * null to avoid consuming more memory for nothing 
    */
   private String ownerId;
   
   private String id;

   private int hash;

   /**
    * The value used in case, the grouping is enabled
    */
   private String group;
   
   /**
    * The full name of the group
    */
   private String fullGroupName;
   
   public CacheKey()
   {
   }

   public CacheKey(String ownerId, String id)
   {
      this(ownerId, id, null);
   }

   public CacheKey(String ownerId, String id, String group)
   {
      this.ownerId = ownerId;
      this.id = id;
      this.hash = calculateHash();
      this.group = group;
   }
   
   /**
    * @return the ownerId
    */
   public final String getOwnerId()
   {
      return ownerId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public final int hashCode()
   {
      return this.hash;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      return getClass().getSimpleName() + "-" + (ownerId == null ? "" : (ownerId + "-")) + id + "-" + group;
   }

   /**
    * {@inheritDoc}
    */
   public final int compareTo(CacheKey o)
   {
      int result = getClass().getName().compareTo(o.getClass().getName());
      if (result == 0 && ownerId != null)
      {
         // The key is of the same type and we assume that the distributed mode is enabled
         result = ownerId.compareTo(o.ownerId);
      }
      return result == 0 ? id.compareTo(o.id) : result;
   }
   
   /**
    * This method is used for the grouping when its enabled. It will return
    * the value of the group if it has been explicitly set otherwise it will
    * return the value of the fullId 
    * @return the group
    */
   @Group
   public final String getGroup()
   {
      if (fullGroupName != null)
      {
         return fullGroupName;
      }
      StringBuilder sb = new StringBuilder();
      if (ownerId != null)
      {
         sb.append(ownerId).append('-');
      }
      return fullGroupName = sb.append(group == null ? id : group).toString();
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf;
      if (ownerId == null)
      {
         out.writeInt(-1);
      }
      else
      {
         buf = ownerId.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }
      if (group == null)
      {
         out.writeInt(-1);
      }
      else
      {
         buf = group.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }

      buf = id.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf;
      int length = in.readInt();
      if (length >= 0)
      {
         buf = new byte[length];
         in.readFully(buf);
         ownerId = new String(buf, Constants.DEFAULT_ENCODING);         
      }
      length = in.readInt();
      if (length >= 0)
      {
         buf = new byte[length];
         in.readFully(buf);
         group = new String(buf, Constants.DEFAULT_ENCODING);         
      }
      buf = new byte[in.readInt()];
      in.readFully(buf);
      id = new String(buf, Constants.DEFAULT_ENCODING);
      hash = calculateHash();
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public final boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      CacheKey cacheKey = (CacheKey)obj;
      if (cacheKey.hash == hash && cacheKey.id.equals(id))
      {
         return ownerId != null ? ownerId.equals(cacheKey.ownerId) : true;
      }
      return false;
   }
   
   /**
    * Calculate the hash.
    * 
    * @return int 
    *            return hash
    */
   private int calculateHash()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
      result = prime * result + (getClass().getName().hashCode());
      return result;
   }
}