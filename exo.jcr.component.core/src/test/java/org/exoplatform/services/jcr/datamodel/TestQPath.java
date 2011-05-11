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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.datamodel;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestQPath.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestQPath extends TestCase
{

   public void testDescendantOrSelfOnSiblings() throws Exception
   {
      // make path
      QPath path1 = QPath.parse("[]:1[]testRoot:1[]node1:4");
      QPath path2 = QPath.parse("[]:1[]testRoot:1[]node1:3");
      QPath child1 = QPath.parse("[]:1[]testRoot:1[]node1:4[]child1:5");

      assertTrue(child1.isDescendantOf(path1));

      assertFalse(child1.isDescendantOf(path2));
   }

}
