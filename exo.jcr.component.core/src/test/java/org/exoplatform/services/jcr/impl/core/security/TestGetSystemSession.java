/**
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.security;

import java.security.AccessControlException;
import java.security.PrivilegedExceptionAction;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class TestGetSystemSession extends BaseSecurityTest
{
   public void testGetSystemSessionSuccess()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getSystemSession();
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able get system session. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testGetSystemSessionFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getSystemSession();
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able get system session.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }
}
