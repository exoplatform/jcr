/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This is the main class of all the mapper used in jcr
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public abstract class AbstractMapper<KOut, VOut> extends
   org.exoplatform.services.ispn.AbstractMapper<CacheKey, Object, KOut, VOut> implements Externalizable
{
   /**
    * This id will be the unique identifier of the workspace in case the
    * distributed mode is enabled as the cache will be then shared so we
    * need this id to prevent mixing data of different workspace. In case
    * the workspace is not distributed the value of this variable will be
    * null to avoid consuming more memory for nothing 
    */
   protected String ownerId;

   public AbstractMapper()
   {
   }

   public AbstractMapper(String ownerId)
   {
      this.ownerId = ownerId;
   }

   /**
    * @see org.exoplatform.services.ispn.AbstractMapper#isValid(java.lang.Object)
    */
   @Override
   protected boolean isValid(CacheKey key)
   {
      return ownerId.equals(key.getOwnerId());
   }

   /**
    * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = ownerId.getBytes("UTF-8");
      out.writeInt(buf.length);
      out.write(buf);
   }

   /**
    * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      ownerId = new String(buf, "UTF-8");
   }
}