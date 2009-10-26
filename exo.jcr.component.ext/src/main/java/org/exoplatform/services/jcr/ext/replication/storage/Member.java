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
package org.exoplatform.services.jcr.ext.replication.storage;

import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 25.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: Member.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class Member
{

   /**
    * Member address.
    */
   private final MemberAddress address;

   /**
    * The priority of member.
    */
   private final int priority;

   /**
    * Member constructor for <code>Subscriber</code>.
    * 
    * @param address
    *          Address (JGroups)
    * @param priority
    *          int
    */
   public Member(MemberAddress address, int priority)
   {
      this.address = address;
      this.priority = priority;
   }

   /**
    * Get member name in format "priority. address". <br/>
    * For information purpose only. Use address and priority in comparisons.
    * 
    * @return String
    * 
    * @see {@link Member.getAddress()}
    */
   public String getName()
   {
      return this.priority + (this.address != null ? " (" + this.address + ")" : "");
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (obj instanceof Member)
         return this.address.equals(((Member)obj).address) && this.priority == ((Member)obj).priority;
      else
         return false;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return super.toString() + " [" + getName() + "]";
   }

   /**
    * Get address of member.
    * 
    * @return Address return address of member
    */
   public MemberAddress getAddress()
   {
      return address;
   }

   /**
    * @return the priority
    */
   public int getPriority()
   {
      return priority;
   }

}
