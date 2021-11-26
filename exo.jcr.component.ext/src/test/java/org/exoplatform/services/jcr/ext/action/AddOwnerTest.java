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

package org.exoplatform.services.jcr.ext.action;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $ Prerequisites: <value> <object
 *          type="org.exoplatform.services.jcr.impl.ext.action.ActionConfiguration"> <field
 *          name="eventTypes"><string>addNode</string></field> <field
 *          name="path"><string>/test</string></field> <field
 *          name="isDeep"><boolean>true</boolean></field> <field
 *          name="actionClassName"><string>org.exoplatform
 *          .services.jcr.ext.action.AddOwneableAction</string></field> </object> </value>
 * 
 * 
 */
public class AddOwnerTest extends BaseStandaloneTest
{

   public void testIfOwnerAdd() throws Exception
   {
      ExtendedNode node = (ExtendedNode)session.getRootNode().addNode("test");
      assertTrue(node.isNodeType("exo:owneable"));
      assertEquals(session.getUserID(), node.getProperty("exo:owner").getString());
   }

   public void testIfNotOwnerAdd() throws Exception
   {
      ExtendedNode node = (ExtendedNode)session.getRootNode().addNode("test2");
      assertFalse(node.isNodeType("exo:owneable"));
   }

}
