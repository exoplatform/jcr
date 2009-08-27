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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS
 * 
 * 03.10.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestUpdate.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestUpdate
   extends JcrAPIBaseTest
{

   private Node testRoot;

   private Session ws1session;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = session.getRootNode().addNode("testUpdate");
      Node ws1node = testRoot.addNode("node1");
      session.save();

      ws1session = repository.login(credentials, "ws1");
      ws1session.getRootNode().addNode("testUpdate");
      ws1session.save();

      ws1session.getWorkspace().clone("ws", ws1node.getPath(), ws1node.getPath(), true);

      Node corrNode = (Node) ws1session.getItem(ws1node.getPath());

      File propData = createBLOBTempFile(1024);
      propData.deleteOnExit();

      InputStream pds = new FileInputStream(propData);
      try
      {
         corrNode.setProperty("prop1", pds);
         corrNode.save();
      }
      catch (RepositoryException e)
      {
         log.error(e);
         pds.close();
         propData.delete();
         ws1session.refresh(false);
      }
   }

   @Override
   protected void tearDown() throws Exception
   {

      ws1session.getItem(testRoot.getPath()).remove();
      ws1session.save();

      testRoot.remove();
      session.save();

      super.tearDown();
   }

   /**
    * The test should be executed with disabled workspace cache!!!
    */
   public void testUpdate() throws Exception
   {
      Node n1 = testRoot.getNode("node1");

      assertFalse("There are not property in ws should be", n1.hasProperty("prop1"));

      n1.update("ws1");

      assertTrue("The property 'prop1' should be in ws", n1.hasProperty("prop1"));
   }
}
