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
package org.exoplatform.services.jcr.api.version;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Random;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionHistory;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 18.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestVersionable.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestVersionable
   extends BaseVersionTest
{

   private Node testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("testRemoveVersionable");
      root.save();
      testRoot.addMixin("mix:versionable");
      root.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.addNode("node1");
      testRoot.addNode("node2").setProperty("prop1", "a property #1");
      testRoot.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.getNode("node1").remove();
      testRoot.save();

      testRoot.checkin();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRoot.remove();
      root.save();

      super.tearDown();
   }

   public void testRemoveMixVersionable() throws Exception
   {

      testRoot.checkout();

      try
      {
         testRoot.removeMixin("mix:versionable");
         testRoot.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("removeMixin(\"mix:versionable\") impossible due to error " + e.getMessage());
      }
   }

   public void testRemoveVersionableFile() throws Exception
   {
      int versionsCount = 5;
      Node testFolder = root.addNode("testVFolader", "nt:folder");
      testFolder.addMixin("mix:versionable");
      Random r = new Random();
      byte[] content = new byte[1024 * 1024];
      r.nextBytes(content);

      Node localSmallFile = testFolder.addNode("smallFile", "nt:file");
      Node contentNode = localSmallFile.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream(content));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      contentNode.addMixin("mix:versionable");
      session.save();
      String vsUuid = contentNode.getProperty("jcr:versionHistory").getString();
      Node vsNode = session.getNodeByUUID(vsUuid);
      // System.out.println(vsNode.getNodes().getSize());
      for (int i = 0; i < versionsCount; i++)
      {
         contentNode.checkin();
         contentNode.checkout();
         r.nextBytes(content);
         contentNode.setProperty("jcr:data", new ByteArrayInputStream(content));

         session.save();
         // /System.out.println(vsNode.getNodes().getSize());

      }
      testFolder.remove();
      session.save();
      try
      {
         session.getNodeByUUID(vsUuid);
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }

   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-437
    * 
    * @throws Exception
    */
   public void testRemoveNonMixVersionableParent() throws Exception
   {
      Node testroot = root.addNode("testRoot");
      Node verionableChild = testroot.addNode("verionableChild");
      verionableChild.addMixin("mix:versionable");
      root.save();
      assertFalse(testroot.isNodeType("mix:versionable"));
      assertTrue(verionableChild.isNodeType("mix:versionable"));

      VersionHistory vHistory = verionableChild.getVersionHistory();
      assertNotNull(vHistory);

      String vhId = ((NodeImpl) vHistory).getUUID();

      assertNotNull(session.getTransientNodesManager().getItemData(vhId));
      testroot.remove();
      root.save();
      assertFalse(root.hasNode("testRoot"));
      ItemData vhdata = session.getTransientNodesManager().getItemData(vhId);
      // !!!!
      assertNull(vhdata);
   }

   public void testRemoveMixVersionableTwice() throws Exception
   {

      testRoot.checkout();

      testRoot.removeMixin("mix:versionable");
      testRoot.save();

      try
      {
         testRoot.removeMixin("mix:versionable");
         fail("removeMixin(\"mix:versionable\") should throw NoSuchNodeTypeException exception");
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }
   }

   public void testIsCheckedOut() throws Exception
   {
      // create versionable subnode and checkin its versionable parent
      // testRoot - versionable ancestor

      testRoot.checkout();
      Node subNode = testRoot.addNode("node1").addNode("node2").addNode("subNode");
      testRoot.save();

      subNode.addMixin("mix:versionable");
      testRoot.save();

      subNode.checkin();
      subNode.checkout();
      subNode.setProperty("property1", "property1 v1");
      subNode.save();
      subNode.checkin();
      subNode.checkout();

      // test
      testRoot.checkin(); // make subtree checked-in
      try
      {
         assertTrue("subNode should be checked-out as it's a mix:versionable", subNode.isCheckedOut());
      }
      catch (RepositoryException e)
      {

      }
   }

}
