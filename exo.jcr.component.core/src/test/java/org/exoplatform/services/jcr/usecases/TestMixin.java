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
package org.exoplatform.services.jcr.usecases;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMixin.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMixin extends BaseUsecasesTest
{

   public void testMixin() throws Exception
   {
      Node a = session.getRootNode().addNode("a");
      a.addMixin("mix:referenceable");

      Property p = a.setProperty("prop", "string");
      Node a1 = p.getParent();

      a1.addMixin("mix:lockable");
      a.addMixin("mix:versionable");
      session.save();

      //check result
      Node a2 = session.getRootNode().getNode("a");
      assertTrue(a2.isNodeType("mix:referenceable"));
      assertTrue(a2.isNodeType("mix:versionable"));
      assertTrue(a2.isNodeType("mix:lockable"));
   }
}
