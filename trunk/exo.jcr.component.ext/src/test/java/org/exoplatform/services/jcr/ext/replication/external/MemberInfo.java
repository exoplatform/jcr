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
package org.exoplatform.services.jcr.ext.replication.external;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: MemberInfo.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class MemberInfo
{
   private final String ipAddress;

   private final int port;

   private final String login;

   private final String password;

   private final int priority;

   public MemberInfo(String ipAddress, int port, String login, String password, int priority)
   {
      this.ipAddress = ipAddress;
      this.port = port;
      this.login = login;
      this.password = password;
      this.priority = priority;
   }

   public String getIpAddress()
   {
      return ipAddress;
   }

   public int getPort()
   {
      return port;
   }

   public String getLogin()
   {
      return login;
   }

   public String getPassword()
   {
      return password;
   }

   public int getPriority()
   {
      return priority;
   }

   public boolean equals(MemberInfo memberInfo)
   {
      return (ipAddress.equals(memberInfo.getIpAddress()) && port == memberInfo.getPort() && priority == memberInfo
         .getPriority());
   }
}
