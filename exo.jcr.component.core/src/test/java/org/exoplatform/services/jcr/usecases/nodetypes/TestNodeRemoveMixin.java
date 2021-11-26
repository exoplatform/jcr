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

   public void testRemoveMixin() throws Exception
   {
      Node rootNode = session.getRootNode();

      Node testNode = rootNode.addNode("testMixinNode1", "exo:JCR-2442");
      testNode.addMixin("exo:archiveable");
      // Set a value to Node's Property
      String restorePath = "test/restore/path";
      testNode.setProperty("exo:restorePath", restorePath);
      session.save();

      try
      {
         testNode.removeMixin("exo:archiveable");
         session.save();
      }
      catch (Exception e)
      {
         fail();
      }
   }
}
