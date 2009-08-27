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
package org.exoplatform.services.jcr.api.nodetypes;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS
 * 
 * Test discusses a same-names issue for properties and child nodes (e.g. case of nt:unstructured).
 * 
 * JSR-170: 6.7.15 Residual Definitions When the name attribute (i.e., that returned by getName())
 * of a PropertyDefinition or NodeDefinition is "*", this indicates that the definition is a
 * residual definition. A residual definition defines the characteristics of all properties (if it
 * is a PropertyDefinition) or child nodes (if it is a NodeDefinition) apart than those explicitly
 * named in other property or node definitions. It is possible for a node type to have more than one
 * residual definition. This means that all properties and child nodes other than those explicitly
 * named must conform to at least one of the residual definitions
 * 
 * JSR-283: 3.3.4 A Property and a Node Can Have the Same Name A property and a node which have the
 * same parent may have the same name. The methods Node.getNode, Session.getNode, Node.getProperty
 * and Session.getProperty obviously specify whether the desired item is a node or a property. The
 * method Session.getItem will return the item at the specified path if there is only one such item,
 * if there is both a node and a property at the specified path, getItem will return the node
 * 
 * 4.7.15 Multiple Definitions with the Same Name A node type may have two or more property
 * definitions with identical name attributes (the value returned by ItemDefinition.getName) as long
 * as the definitions are otherwise distinguishable by either the required type attribute (the value
 * returned by PropertyDefinition.getRequiredType) or the multiple attribute (the value returned by
 * PropertyDefinition.isMultiple). Similarly, a node type may have two or more child node
 * definitions with identical name attributes as long as they are distinguishable by the required
 * primary types attribute (the value returned by NodeDefinition.getRequiredPrimaryTypes). A node
 * type may have a property definition and child node definition with identical name attributes (see
 * 3.3.4 A Property and a Node Can Have the Same Name).
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSameNameItems.java 12855 2008-04-07 14:47:20Z pnedonosko $
 */
public class TestSameNameItems
   extends JcrAPIBaseTest
{

   private Node testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("residialTest", "nt:unstructured");
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRoot.remove();
      root.save();

      super.tearDown();
   }

   public void testSameNames() throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException
   {

      try
      {
         Node itemNode = testRoot.addNode("item");

         testRoot.save();

         Property itemProperty = testRoot.setProperty("item", "content");

         testRoot.save();

         assertEquals("Nodes should be same", itemNode, testRoot.getNode("item"));
         assertEquals("Properties should be same", itemProperty, testRoot.getProperty("item"));

         // replace same-name property with same-name node, i.e. node[2]
         testRoot.getProperty("item").remove();
         Node itemNode2 = testRoot.addNode("item");
         testRoot.save();

         assertEquals("Nodes should be same", itemNode, testRoot.getNode("item"));
         assertEquals("Nodes should be same", itemNode2, testRoot.getNode("item[2]"));
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("No error should be thrown, but " + e);
      }
   }

}
