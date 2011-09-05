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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.statistics;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 12 avr. 2010  
 */
public class TestJCRStatisticsManager extends TestCase
{

   public void testFormatName()
   {
      assertNull(JCRStatisticsManager.formatName(null));
      assertEquals("myMethod(String, String)", JCRStatisticsManager.formatName("myMethod(String, String)"));
      assertEquals("myMethod(String, String)", JCRStatisticsManager.formatName("  myMethod  (String ,   String)  "));
      assertEquals("myMethod(String, String)", JCRStatisticsManager.formatName("myMethod(String;String)"));
      assertEquals("myMethod(String, String)", JCRStatisticsManager.formatName("myMethod(String,String)"));
   }
}
