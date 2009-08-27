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
package org.exoplatform.services.jcr.api.reading;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS
 * 
 * Subject of tests see: 4 The Repository Model preface, 6.7.16 Value Constraints, 8.5.2.3 Path
 * Literals
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestItemLocation.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestItemLocation
   extends JcrAPIBaseTest
{

   private Node testNode;

   private final double DOUBLE_VALUE1 = 1234.4321;

   private final double DOUBLE_VALUE2 = 4567.7654;

   private final String STRING_VALUE1 = "STRING_VALUE1";

   private final String STRING_VALUE2 = "STRING_VALUE2";

   public void setUp() throws Exception
   {

      super.setUp();

      testNode = root.addNode("testNode");
      Node n1 = testNode.addNode("n1");
      Node n1_1 = n1.addNode("n1-n1");
      Node n1_1_1 = n1_1.addNode("n1-n1-n1");
      n1_1_1.setProperty("n1-n1-n1-p1", DOUBLE_VALUE1);
      n1_1_1.setProperty("n1-n1-n1-p2", STRING_VALUE1);

      Node n1_2 = n1.addNode("n1-n2");
      Node n1_2_1 = n1_2.addNode("n1-n2-n1");
      n1_2_1.setProperty("n1-n2-n1-p1", DOUBLE_VALUE2);
      n1_2_1.setProperty("n1-n2-n1-p2", STRING_VALUE2);

      root.save();
   }

   public void tearDown() throws Exception
   {
      testNode.remove();
      root.save();

      super.tearDown();
   }

   public void testRelativePath() throws RepositoryException
   {

      try
      {
         testNode.getNode("n1");
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         testNode.getNode("n1/n1-n1");
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         testNode.getNode("n1/n1-n1/n1-n1-n1");
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", DOUBLE_VALUE1, testNode.getProperty(
                  "n1/n1-n1/n1-n1-n1/n1-n1-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", STRING_VALUE1, testNode.getProperty(
                  "n1/n1-n1/n1-n1-n1/n1-n1-n1-p2").getString());
      }
      catch (RepositoryException e)
      {
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         testNode.getNode("n1/n1-n2");
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         testNode.getNode("n1/n1-n2/n1-n2-n1");
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", DOUBLE_VALUE2, testNode.getProperty(
                  "n1/n1-n2/n1-n2-n1/n1-n2-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", STRING_VALUE2, testNode.getProperty(
                  "n1/n1-n2/n1-n2-n1/n1-n2-n1-p2").getString());
      }
      catch (RepositoryException e)
      {
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }

   }

   public void testRelativePath_GetItSelf() throws RepositoryException
   {

      Node n1 = null;
      Node n1_1 = null;

      try
      {
         n1 = testNode.getNode("n1");
         n1_1 = testNode.getNode("n1/n1-n1");
      }
      catch (RepositoryException e)
      {
         fail("Child nodes must be accessible and readable. But error occurs: " + e);
      }

      // get itself
      try
      {
         assertEquals("Nodes must be equals", n1_1, n1_1.getNode("."));
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Node must be accessible and readable. But error occurs: " + e);
      }
   }

   public void testRelativePath_GetParent() throws RepositoryException
   {

      Node n1 = null;
      Node n1_1 = null;

      try
      {
         n1 = testNode.getNode("n1");
         n1_1 = testNode.getNode("n1/n1-n1");
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child nodes must be accessible and readable. But error occurs: " + e);
      }

      // get parent
      try
      {
         assertEquals("Nodes must be equals", n1, n1_1.getNode(".."));
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Node must be accessible and readable. But error occurs: " + e);
      }

      // get itself from parent
      try
      {
         assertEquals("Nodes must be equals", n1_1, n1_1.getNode("../n1-n1"));
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Node must be accessible and readable. But error occurs: " + e);
      }
      try
      {
         assertEquals("Property values must be equals", DOUBLE_VALUE1, n1_1
                  .getProperty("../n1-n1/n1-n1-n1/n1-n1-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
   }

   public void testRelativePath_GetAnotherSubtree() throws RepositoryException
   {

      Node n1 = null;
      Node n1_1 = null;

      try
      {
         n1 = testNode.getNode("n1");
         n1_1 = testNode.getNode("n1/n1-n1");
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child nodes must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         n1_1.getNode("../n1-n2/n1-n2-n1");
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", DOUBLE_VALUE2, n1_1
                  .getProperty("../n1-n2/n1-n2-n1/n1-n2-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
      try
      {
         assertEquals("Property values must be equals", STRING_VALUE2, n1_1
                  .getProperty("../n1-n2/n1-n2-n1/n1-n2-n1-p2").getString());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
   }

   public void testRelativePath_GetAnotherSubtreeDeep() throws RepositoryException
   {

      Node n1 = null;
      Node n1_1_1 = null;

      try
      {
         n1 = testNode.getNode("n1");
         n1_1_1 = testNode.getNode("n1/n1-n1/n1-n1-n1");
      }
      catch (RepositoryException e)
      {
         fail("Child nodes must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         Node n1_2_1 = n1_1_1.getNode("../../n1-n2/n1-n2-n1");
         assertEquals("Property values must be equals", DOUBLE_VALUE2, n1_2_1.getProperty("n1-n2-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         assertEquals("Property values must be equals", DOUBLE_VALUE2, n1_1_1.getProperty(
                  "../../n1-n2/n1-n2-n1/n1-n2-n1-p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
      try
      {
         assertEquals("Property values must be equals", STRING_VALUE2, n1_1_1.getProperty(
                  "../../n1-n2/n1-n2-n1/n1-n2-n1-p2").getString());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
   }

   public void testRelativePath_Create() throws RepositoryException
   {

      Node n1 = null;

      try
      {
         n1 = testNode.getNode("n1");
      }
      catch (RepositoryException e)
      {
         fail("Child nodes must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         Node n1_1 = n1.addNode("./n1--n1");
         n1.save();
         assertEquals("Nodes must be equals", n1_1, n1.getNode("./n1--n1"));
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }
      finally
      {
         n1.getNode("./n1--n1").remove();
         n1.save();
      }

      try
      {
         n1.addNode(".");
         n1.save();
         fail("Can't add node with relPath to itself");
      }
      catch (RepositoryException e)
      {
         // ok
      }

      try
      {
         Node n1_1 = n1.addNode("./n1--n1");
         n1_1.setProperty("n1--n1--p1", DOUBLE_VALUE1); // n1.getNodes()
         n1.save();

         assertEquals("Property values must be equals", DOUBLE_VALUE1, n1.getProperty("n1--n1/n1--n1--p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }
      finally
      {
         n1.getNode("./n1--n1").remove();
         n1.save();
      }

      try
      {
         n1.addNode("./n1--n1");
         Node n1_2 = n1.addNode("./n1--n1/../n1--n2");
         n1_2.setProperty("n1--n2--p1", DOUBLE_VALUE1); // actually n1/n1--n2/n1--n2--p1
         n1.save();

         assertEquals("Property values must be equals", DOUBLE_VALUE1, n1.getProperty("n1--n2/n1--n2--p1").getDouble());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Child property must be accessible and readable. But error occurs: " + e);
      }

   }

}
