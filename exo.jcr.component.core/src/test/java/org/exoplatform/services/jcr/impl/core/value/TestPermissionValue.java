/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.value;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.access.AccessControlEntry;

public class TestPermissionValue extends TestCase
{

   public void testParse() throws Exception
   {
      AccessControlEntry accessEntry = AccessControlEntry.parse("root read");
      assertEquals("root", accessEntry.getIdentity());
      assertEquals("read", accessEntry.getPermission());
      // Identity with space
      String identity = "m ar  y";
      accessEntry = AccessControlEntry.parse(identity + " read");
      assertEquals(identity, accessEntry.getIdentity());
      assertEquals("read", accessEntry.getPermission());
   }

   public void testAsString() throws Exception
   {
      assertEquals("root read", new PermissionValue("root", "read").getString());
      // Identity with space
      String identity = "m ar  y";
      assertEquals(identity + " read", new PermissionValue(identity, "read").getString());
   }
}
