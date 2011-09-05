/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.owner;

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
 *          .services.jcr.ext.owner.AddOwneableAction</string></field> </object> </value>
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
