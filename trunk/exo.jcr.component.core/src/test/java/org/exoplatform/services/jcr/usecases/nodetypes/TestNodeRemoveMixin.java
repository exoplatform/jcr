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
package org.exoplatform.services.jcr.usecases.nodetypes;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Anh Nguyen ntuananh.vn@gmail.com Nov 13, 2007
 */
public class TestNodeRemoveMixin extends BaseUsecasesTest
{

   public void testNodeRemoveMixin() throws Exception
   {

      // Register Nodetypes - performed in configuration

      // Create Node
      Node rootNode = session.getRootNode();
      Node testNode = rootNode.addNode("testMixinNode", "exo:myType");

      // Add mixin to Node
      testNode.addMixin("mix:versionable");
      testNode.addMixin("exo:archiveable");

      // Set a value to Node's Property
      String restorePath = "test/restore/path";
      testNode.setProperty("exo:restorePath", restorePath);

      rootNode.save();

      assertTrue(testNode.isNodeType("exo:archiveable"));
      assertNotNull(testNode.getProperty("exo:restorePath"));

      // Do remove Mixin from Node
      testNode = rootNode.getNode("testMixinNode");
      assertNotNull(testNode.getProperties());
      testNode.removeMixin("exo:archiveable");
      session.save();

      // Error should not be here! // WRONG, node already has at least one property jcr:primaryType
      // assertNotNull(testNode.getProperties());

      assertFalse(testNode.hasProperty("exo:restorePath"));
   }
}
