/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl;

import org.exoplatform.services.jcr.impl.util.XPathUtils;

import junit.framework.TestCase;

public class XPathUtilsTest extends TestCase {

  public void testNull () {
    assertNull(XPathUtils.escapeIllegalXPathName(null));
  }
  
  public void testAtSign () {
    String hexString;
    hexString = XPathUtils.escapeIllegalXPathName("@testme");
    assertEquals("At sign is not convertible",hexString,"_x0040_testme");
    hexString = XPathUtils.escapeIllegalXPathName("/@testme");
    assertEquals("At sign is not convertible",hexString,"/_x0040_testme");
    hexString = XPathUtils.escapeIllegalXPathName("/alex.goodman@testme");
    assertEquals("At sign is not convertible",hexString,"/alex.goodman_x0040_testme");
  }
  
  public void testNumeric() {
    String hexString;
    hexString = XPathUtils.escapeIllegalXPathName("123456789");
    assertEquals("Cannot convert path starting with numberic",hexString ,"_x0031_23456789");
    hexString = XPathUtils.escapeIllegalXPathName("/123456789");
    assertEquals("Cannot convert path starting with numberic",hexString ,"/_x0031_23456789");
    hexString = XPathUtils.escapeIllegalXPathName("/12345/6789");
    assertEquals("Cannot convert path starting with numberic",hexString ,"/_x0031_2345/_x0036_789");
  }
  
  public void testEscapeIllegalSQLName() {
    assertNull(XPathUtils.escapeIllegalSQLName(null));
    assertEquals("", XPathUtils.escapeIllegalSQLName(""));
    //
    assertEquals("''", XPathUtils.escapeIllegalSQLName("'"));
    assertEquals("It''s a foo", XPathUtils.escapeIllegalSQLName("It's a foo"));
    assertEquals("/a/b''/c", XPathUtils.escapeIllegalSQLName("/a/b'/c"));
  }
}
