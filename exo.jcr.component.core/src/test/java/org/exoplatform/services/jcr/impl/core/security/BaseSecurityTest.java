/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.security;

import org.exoplatform.services.jcr.BaseStandaloneTest;

import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Enumeration;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public abstract class BaseSecurityTest extends BaseStandaloneTest
{

   public void setUp() throws Exception
   {
      super.setUp();
      SecurityManager security = System.getSecurityManager();
      assertNotNull("SecurityManager must be ON.", security);
   }

   public String getRepositoryName()
   {
      return "db1";
   }

   /**
    * Run privileged action with specified privileges.
    */
   protected <T> T doPrivilegedAction(PrivilegedExceptionAction<T> action, ProtectionDomain[] protectionDomains)
      throws Throwable
   {
      try
      {
         return AccessController.doPrivileged(action, new AccessControlContext(protectionDomains));
      }
      catch (PrivilegedActionException pae)
      {
         throw pae.getCause();
      }
   }

   /**
    * Run privileged action without any privileges.
    */
   protected <T> T doPrivilegedAction(PrivilegedExceptionAction<T> action) throws Throwable
   {
      ProtectionDomain[] protectionDomains =
         new ProtectionDomain[]{new ProtectionDomain(new CodeSource(getCodeSource(),
            (java.security.cert.Certificate[])null), new Permissions())};
      try
      {
         return AccessController.doPrivileged(action, new AccessControlContext(protectionDomains));
      }
      catch (PrivilegedActionException pae)
      {
         throw pae.getCause();
      }
   }

   /**
    * Run privileged action with static permissions only.
    */
   protected <T> T doPrivilegedActionStaticPermissions(PrivilegedExceptionAction<T> action) throws Throwable
   {
      try
      {
         return AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         throw pae.getCause();
      }
   }

   /**
    * Run privileged action with specified privileges.
    */
   protected <T> T doPrivilegedAction(PrivilegedAction<T> action, ProtectionDomain[] protectionDomains)
   {
      return AccessController.doPrivileged(action, new AccessControlContext(protectionDomains));
   }

   /**
    * Run privileged action without any privileges.
    */
   protected <T> T doPrivilegedAction(PrivilegedAction<T> action)
   {
      ProtectionDomain[] protectionDomains =
         new ProtectionDomain[]{new ProtectionDomain(new CodeSource(getCodeSource(),
            (java.security.cert.Certificate[])null), new Permissions())};
      return AccessController.doPrivileged(action, new AccessControlContext(protectionDomains));
   }

   /**
    * Run privileged action with static permissions only.
    */
   protected <T> T doPrivilegedActionStaticPermissions(PrivilegedAction<T> action)
   {
      return AccessController.doPrivileged(action);
   }

   /**
    * Get code-source of class.
    */
   protected URL getCodeSource()
   {
      return getClass().getProtectionDomain().getCodeSource().getLocation();
   }

   protected static final PermissionCollection ALL = new PermissionCollection()
   {

      public boolean implies(Permission permission)
      {
         return true;
      }

      public Enumeration<Permission> elements()
      {
         return new Enumeration<Permission>()
         {
            private boolean hasMore = true;

            public boolean hasMoreElements()
            {
               return hasMore;
            }

            public Permission nextElement()
            {
               hasMore = false;
               return new AllPermission();
            }
         };
      }

      public void add(Permission permission)
      {
      }
   };

}
