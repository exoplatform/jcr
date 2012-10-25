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
package org.exoplatform.services.jcr.impl.quota;

import junit.framework.TestCase;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TestNodePathPattern.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestPathPatternUtils extends TestCase
{
   /**
    * Pattern testing.
    */
   public void testPattern() throws Exception
   {
      assertTrue(PathPatternUtils.acceptDescendant("/", "/"));
      assertTrue(PathPatternUtils.acceptDescendant("/", "/a"));
      assertTrue(PathPatternUtils.acceptDescendant("/", "/a/c"));
      assertTrue(PathPatternUtils.acceptName("/", "/"));
      assertFalse(PathPatternUtils.acceptName("/", "/a"));
      assertFalse(PathPatternUtils.acceptName("/", "/a/c"));

      assertTrue(PathPatternUtils.acceptDescendant("/a", "/a"));
      assertTrue(PathPatternUtils.acceptDescendant("/a", "/a/c"));
      assertTrue(PathPatternUtils.acceptDescendant("/a", "/a/c/"));
      assertFalse(PathPatternUtils.acceptDescendant("/a", "/b"));
      assertFalse(PathPatternUtils.acceptDescendant("/a", "/b/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/a", "/"));
      assertTrue(PathPatternUtils.acceptName("/a", "/a"));
      assertTrue(PathPatternUtils.acceptName("/a", "/a/"));
      assertFalse(PathPatternUtils.acceptName("/a", "/a/c/"));

      assertTrue(PathPatternUtils.acceptDescendant("/a/b", "/a/b"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/b", "/a/b/c"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/b", "/a/b/c/"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/b", "/a/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/b", "/a"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/b", "/"));
      assertTrue(PathPatternUtils.acceptName("/a/b", "/a/b"));
      assertFalse(PathPatternUtils.acceptName("/a/b", "/a/b/c"));
      assertFalse(PathPatternUtils.acceptName("/a/b", "/a/b/c/"));

      assertTrue(PathPatternUtils.acceptDescendant("/*", "/aaa"));
      assertTrue(PathPatternUtils.acceptDescendant("/*", "/aaa/bbb"));
      assertFalse(PathPatternUtils.acceptDescendant("/*", "/"));
      assertTrue(PathPatternUtils.acceptName("/*", "/aaa"));
      assertFalse(PathPatternUtils.acceptName("/*", "/aaa/bbb"));

      assertTrue(PathPatternUtils.acceptDescendant("/a/*", "/a/bbb"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/*", "/a/bbb/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*", "/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*", "/"));
      assertTrue(PathPatternUtils.acceptName("/a/*", "/a/bbb"));
      assertFalse(PathPatternUtils.acceptName("/a/*", "/a/bbb/c"));

      assertTrue(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a/ccc/b/ddd"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a/ccc/b/ddd/ggg"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/d/ccc/b/ddd"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a/ccc/e/ddd"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a/ccc/b"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a/ccc"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/a"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/*/b/*", "/"));
      assertTrue(PathPatternUtils.acceptName("/a/*/b/*", "/a/ccc/b/ddd"));
      assertFalse(PathPatternUtils.acceptName("/a/*/b/*", "/a/ccc/b/ddd/ggg"));

      assertTrue(PathPatternUtils.acceptDescendant("/%", "/a/c"));
      assertTrue(PathPatternUtils.acceptDescendant("/%", "/a"));
      assertFalse(PathPatternUtils.acceptDescendant("/%", "/aa"));
      assertFalse(PathPatternUtils.acceptDescendant("/%", "/aa/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/%", "/"));
      assertTrue(PathPatternUtils.acceptName("/%", "/a"));
      assertFalse(PathPatternUtils.acceptName("/%", "/a/c"));

      assertTrue(PathPatternUtils.acceptDescendant("/%%%", "/aaa"));
      assertTrue(PathPatternUtils.acceptDescendant("/%%%", "/aaa/bbb"));
      assertFalse(PathPatternUtils.acceptDescendant("/%%%", "/aaaa"));
      assertFalse(PathPatternUtils.acceptDescendant("/%%%", "/a"));
      assertFalse(PathPatternUtils.acceptDescendant("/%%%", "/a/b/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/%%%", "/"));
      assertTrue(PathPatternUtils.acceptName("/%%%", "/aaa"));
      assertFalse(PathPatternUtils.acceptName("/%%%", "/aaa/b"));

      assertTrue(PathPatternUtils.acceptDescendant("/a/%%", "/a/bb"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/%%", "/a/bb/c"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/%%", "/a/bbb"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/%%", "/a"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/%%", "/"));
      assertTrue(PathPatternUtils.acceptName("/a/%%", "/a/bb"));
      assertFalse(PathPatternUtils.acceptName("/a/%%", "/a/bb/c"));

      assertTrue(PathPatternUtils.acceptDescendant("/a/%%/b/*", "/a/cb/b/eee"));
      assertTrue(PathPatternUtils.acceptDescendant("/a/%%/b/*", "/a/dc/b/eee/ggg"));
      assertFalse(PathPatternUtils.acceptDescendant("/a/%%/b/*", "/a/cc/d/eee/ggg"));
      assertTrue(PathPatternUtils.acceptName("/a/%%/b/*", "/a/cc/b/eee"));
      assertFalse(PathPatternUtils.acceptName("/a/%%/b/*", "/a/cc/b/eee/ggg"));
   }

   /**
    * Pattern testing.
    */
   public void testExtractCommonAncestor() throws Exception
   {
      assertEquals("/a/b", PathPatternUtils.extractCommonAncestor("/a/*", "/a/b/c"));
      assertEquals("/a/b", PathPatternUtils.extractCommonAncestor("/a/*", "/a/b"));
      assertEquals("/a/b", PathPatternUtils.extractCommonAncestor("/a/b/*", "/a/b"));
      assertEquals("/b", PathPatternUtils.extractCommonAncestor("/*", "/b/c/d"));
      assertEquals("/", PathPatternUtils.extractCommonAncestor("/a/*", "/b/c/d"));
      assertEquals("/", PathPatternUtils.extractCommonAncestor("/a", "/b"));
   }
}
