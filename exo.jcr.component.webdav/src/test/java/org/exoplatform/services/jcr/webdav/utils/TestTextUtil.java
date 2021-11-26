/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.webdav.utils;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.webdav.util.TextUtil;

/**
 * @author <a href="mailto:areshetnyak@exoplatform.com">Alex Reshetnyak</a>
 * @version $Id: rainf0x $
 *
 */
public class TestTextUtil extends TestCase
{
   public void testParentPath() throws Exception
   {
      assertEquals("/a/b",TextUtil.parentPath("/a/b/c.txt"));
      assertEquals("/a",TextUtil.parentPath("/a/b"));
      assertEquals("/",TextUtil.parentPath("/a"));
      assertEquals("/",TextUtil.parentPath("/"));
      
      try
      {
         TextUtil.parentPath("a");
         fail("A IllegalArgumentException is expected here");
      }
      catch (IllegalArgumentException e)
      {
      }
   }
   
   public void testUnescape()
   {
      String filename = "\u00E1\u00E1\u00E1\u00E1\u00E1.txt";
      assertEquals(filename, TextUtil.unescape(filename, '%'));
   }
}
