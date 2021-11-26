/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.utils;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.util.StringConverter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestStringConverter extends TestCase
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestStringConverter");

   public void testNormalize_() throws Exception
   {
      assertEquals("My_x0", StringConverter.normalizeString("My_x0", true));
      assertEquals("My_x0020_Documents", StringConverter.normalizeString("My Documents", true));
      assertEquals("My_Documents", StringConverter.normalizeString("My_Documents", true));
      assertEquals("My_x0020Documents", StringConverter.normalizeString("My_x0020Documents", true));
      assertEquals("My_x005f_x0020_Documents", StringConverter.normalizeString("My_x0020_Documents", true));
   }
}
