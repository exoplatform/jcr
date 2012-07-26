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
package org.exoplatform.services.jcr.webdav.utils;

import org.exoplatform.services.jcr.webdav.util.TextUtil;

import junit.framework.TestCase;

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
   }
}
