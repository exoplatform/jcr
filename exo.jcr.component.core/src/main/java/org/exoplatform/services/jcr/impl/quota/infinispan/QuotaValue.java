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
package org.exoplatform.services.jcr.impl.quota.infinispan;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Wraps quota limit value and asynchronize update value into
 * one common class.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: QuotaValue.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class QuotaValue implements Externalizable
{
   private boolean asyncUpdate;

   private long quotaLimit;

   /**
    * Constructor for serialization.
    */
   public QuotaValue()
   {
   }

   /**
    * QuotaValue constructor.
    */
   public QuotaValue(long quotaLimit, boolean asyncUpdate)
   {
      this.quotaLimit = quotaLimit;
      this.asyncUpdate = asyncUpdate;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeLong(quotaLimit);
      out.writeBoolean(asyncUpdate);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      quotaLimit = in.readLong();
      asyncUpdate = in.readBoolean();
   }

   /**
    * Getter for {@link #asyncUpdate}.
    */
   boolean getAsyncUpdate()
   {
      return asyncUpdate;
   }

   /**
    * Getter for {@link #quotaLimit}.
    */
   long getQuotaLimit()
   {
      return quotaLimit;
   }
}
