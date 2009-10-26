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

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 25.12.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: StateEvent.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class StateEvent
{

   /**
    * The current members.
    */
   private final List<MemberAddress> members;

   /**
    * The local member.
    */
   private final MemberAddress localMember;

   /**
    * @return the members
    */
   public List<MemberAddress> getMembers()
   {
      return members;
   }

   /**
    * StateEvent  constructor.
    *
    * @param localMember
    *          MemberAddress, the local member
    * @param members
    *          List, the memebrs
    */
   public StateEvent(MemberAddress localMember, List<MemberAddress> members)
   {
      this.members = members;
      this.localMember = localMember;
   }

   /**
    * getLocalMember.
    *
    * @return MemberAddress
    *           return the local member
    */
   public MemberAddress getLocalMember()
   {
      return localMember;
   }

   /**
    * isCoordinator.
    *
    * @return boolean
    *           return 'true' when local member is coordinator.  
    */
   public boolean isCoordinator()
   {
      return members.get(0).getAddress().equals(localMember.getAddress());
   }
}
