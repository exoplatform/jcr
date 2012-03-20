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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.services.jcr.impl.Constants;
import org.infinispan.distribution.group.Group;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS. <br/>
 * Base class for WorkspaceCache keys.<br/>
 * 
 * Date: 10.06.2008<br/>
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: CacheKey.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public abstract class CacheKey implements Externalizable, Comparable<CacheKey>
{

   protected String id;

   protected int hash;

   /**
    * The value used in case, the grouping is enabled
    */
   protected String group;
   
   public CacheKey()
   {
   }

   public CacheKey(String id)
   {
      this(id, null);
   }

   public CacheKey(String id, String group)
   {
      this.id = id;
      this.hash = id.hashCode();
      this.group = group;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      return this.hash;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + "-" + this.id + "-" + this.group;
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(CacheKey o)
   {
      int result = getClass().getName().compareTo(o.getClass().getName());
      return result == 0 ? id.compareTo(o.id) : result;
   }
   
   /**
    * This method is used for the grouping when its enabled. It will return
    * the value of the group if it has been explicitly set otherwise it will
    * return the value of the fullId 
    * @return the group
    */
   @Group
   public String getGroup()
   {
      return group == null ? id : group;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf;
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
         group = new String(buf, Constants.DEFAULT_ENCODING);         
      }

      buf = new byte[in.readInt()];
      in.readFully(buf);
      id = new String(buf, Constants.DEFAULT_ENCODING);
      hash = id.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public abstract boolean equals(Object obj);
}