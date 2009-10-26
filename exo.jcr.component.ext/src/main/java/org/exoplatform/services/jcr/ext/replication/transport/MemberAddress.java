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
package org.exoplatform.services.jcr.ext.replication.transport;

import org.jgroups.Address;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Wrapper around JGroups Address (To have clean API).
 * 
 * <br/>
 * Date: 25.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: Member.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class MemberAddress implements Externalizable
{

   /**
    * The JGroups address.
    */
   private Address address;

   /**
    * Member constructor.
    * 
    * @param address
    *          Address (JGroups)
    */
   public MemberAddress(Address address)
   {
      this.address = address;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (obj instanceof MemberAddress)
         return this.address.equals(((MemberAddress)obj).address);
      else
         return false;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return super.toString() + " [" + getAddress() + "]";
   }

   /**
    * Get address of member.
    * 
    * @return Address return address of member
    */
   public Address getAddress()
   {
      return address;
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      address = (Address)in.readObject();
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeObject(address);
   }
}
