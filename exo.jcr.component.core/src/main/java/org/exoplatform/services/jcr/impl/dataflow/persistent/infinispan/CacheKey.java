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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS. <br/>
 * RE
 * Base class for WorkspaceCache keys.<br/>
 * 
 * Date: 10.06.2008<br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheKey.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public abstract class CacheKey implements Externalizable, Comparable<CacheKey>
{

   protected String fullId;

   protected int hash;

   public CacheKey()
   {
   }

   public CacheKey(String id)
   {
      this(id, id.hashCode());
   }

   public CacheKey(String id, int hash)
   {
      this.fullId = this.getClass().getSimpleName() + "-" + id;
      this.hash = hash;
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
      return this.fullId;
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(CacheKey o)
   {
      return fullId.compareTo(o.fullId);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(hash);

      byte[] buf = fullId.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      hash = in.readInt();

      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      fullId = new String(buf, Constants.DEFAULT_ENCODING);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public abstract boolean equals(Object obj);
}