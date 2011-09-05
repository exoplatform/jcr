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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 27.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSameNameSiblingsReindex.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestSameNameSiblingsReindex extends JcrAPIBaseTest
{

   protected final String TEST_ROOT = "reindex_test";

   private Node testBase = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testBase = root.addNode(TEST_ROOT);
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         testBase.remove();
         root.save();
      }
      catch (Throwable e)
      {
         log.error("TEAR DOWN ERROR. " + getName() + ". " + e.getMessage(), e);
      }

      super.tearDown();
   }

   // -------------- Tests ---------------

   /**
    * Check if reindex works properly, i.e. nodes will be stored well in persistent storage
    */
   public void testSNSRemove() throws Exception
   {

      testBase.addNode("n1");
      testBase.addNode("n1");
      Node n3 = testBase.addNode("n1");
      Node n4 = testBase.addNode("n1");
      n4.addMixin("mix:referenceable");
      String n4uuid = n4.getUUID();
      testBase.addNode("n1");

      root.save();

      n3.remove();

      root.save();

      try
      {
         n3 = testBase.getNode("n1[3]");
         if (log.isDebugEnabled())
            log.debug("Third node is " + n3.getPath() + ", " + n3.getIndex());

         try
         {
            assertEquals("Node /" + TEST_ROOT + "/n1[3] has a wrong UUID", n4uuid, n3.getUUID());
         }
         catch (RepositoryException e)
         {
            fail("Node /" + TEST_ROOT + "/n1[3] must be mix:referenceable as result of reindex n1[4] to n1[3]");
         }
      }
      catch (PathNotFoundException e)
      {
         fail("Node /"
            + TEST_ROOT
            + "/n1[3] is not found after remove. But must as result of reindex n1[4] to n1[3]. Case Node.getNode(String)");
      }

      try
      {
         n3 = (Node)testBase.getSession().getItem("/" + TEST_ROOT + "/n1[3]");
         if (log.isDebugEnabled())
            log.debug("Third node is " + n3.getPath() + ", " + n3.getIndex());

         try
         {
            assertEquals("Node /" + TEST_ROOT + "/n1[3] has a wrong UUID", n4uuid, n3.getUUID());
         }
         catch (RepositoryException e)
         {
            fail("Node /" + TEST_ROOT + "/n1[3] must be mix:referenceable as result of reindex n1[4] to n1[3]");
         }
      }
      catch (PathNotFoundException e)
      {
         fail("Node /"
            + TEST_ROOT
            + "/n1[3] is not found after remove. But must as result of reindex n1[4] to n1[3]. Case Session.getItem(String)");
      }
   }

   /**
    * ISSUE: sub node dereferenced when deleting "same name sibling"
    * http://jira.exoplatform.org/browse/JCR-120
    */
   public void testSubNodeDereferenced() throws Exception
   {
      // create siblings and sub-nodes
      Node siblings[] = new Node[]{testBase.addNode("node"), testBase.addNode("node")};
      for (Node n : siblings)
      {
         n.addNode("sub1").addNode("sub1.1");
      }
      testBase.save();
      // remove one sibling
      siblings[0].remove();
      testBase.save();

      // test remaining sibling
      assertTrue(siblings[1].hasNode("sub1"));
      assertTrue(siblings[1].hasNode("sub1/sub1.1")); // test fails: sub-node dereferenced !
   }

}
