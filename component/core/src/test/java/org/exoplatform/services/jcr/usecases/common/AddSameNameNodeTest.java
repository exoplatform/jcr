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
package org.exoplatform.services.jcr.usecases.common;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AddSameNameNodeTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class AddSameNameNodeTest
   extends BaseUsecasesTest
{

   private Node testRoot = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = session.getRootNode().addNode("AddSameNameNode test");
      session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {

      if (session.getRootNode().hasNode(testRoot.getName()))
      {
         testRoot.remove();
         session.save();
      }

      super.tearDown();
   }

   public void testAddSameNameNode() throws Exception
   {

      Node file = testRoot.addNode("file1", "nt:file");

      Node content = file.addNode("jcr:content", "nt:unstructured");

      content.setProperty("any property", "any content");

      testRoot.save();

      try
      {
         assertEquals("Content must be equals", testRoot.getProperty("file1/jcr:content/any property").getString(),
                  "any content");
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }

      try
      {
         assertFalse("The node shouldn't has mix:versionable", testRoot.getNode("file1").isNodeType("mix:versionable"));
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }

      // add second node with same name

      Node file1 = testRoot.addNode("file1", "nt:file");

      Node content1 = file1.addNode("jcr:content", "nt:unstructured");

      content1.setProperty("any property", "any content 1");

      testRoot.save();

      try
      {
         assertEquals("Content must be equals", testRoot.getProperty("file1[2]/jcr:content/any property").getString(),
                  "any content 1");
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }

      // index 2 mixins
      try
      {
         assertFalse("The node shouldn't has mix:versionable", testRoot.getNode("file1[2]").isNodeType(
                  "mix:versionable"));
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }

      // index 1 mixins
      try
      {
         assertFalse("The node shouldn't has mix:versionable", testRoot.getNode("file1").isNodeType("mix:versionable"));
      }
      catch (PathNotFoundException e)
      {
         fail(e.getMessage());
      }

   }
}
