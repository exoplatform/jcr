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

import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

/**
 * Tests the mix:versionable remove, remove of version history in case of remove of the last
 * versionable node.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestVersionableRemove.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestVersionableRemove
   extends BaseVersionTest
{

   Session ws1Session = null;

   Session ws2Session = null;

   Node ws1TestRoot = null;

   Node ws2TestRoot = null;

   Node ws1Versionable = null;

   Node ws2Versionable = null;

   String versionHistoryUUID = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      // ws1
      ws1Session = repository.login(credentials, "ws1");
      ws1TestRoot = ws1Session.getRootNode().addNode("testVHRoot");

      ws1Versionable = ws1TestRoot.addNode("ws1Versionable");
      ws1Versionable.addMixin("mix:versionable");

      ws1Session.save();

      // create a version
      Version v1 = ws1Versionable.checkin();
      ws1Versionable.checkout();

      versionHistoryUUID = ws1Versionable.getProperty("jcr:versionHistory").getString();

      // ws2
      ws2Session = repository.login(credentials, "ws2");
      ws2TestRoot = ws2Session.getRootNode().addNode("testVHRoot");

      ws2Session.save();

      // corresponding node (with common version history)
      ws2Session.getWorkspace().clone("ws1", ws1Versionable.getPath(), ws2TestRoot.getPath() + "/ws2Versionable", true);

      ws2Versionable = ws2TestRoot.getNode("ws2Versionable");
      Version v2 = ws2Versionable.checkin();
      ws2Versionable.checkout();
   }

   @Override
   protected void tearDown() throws Exception
   {

      ws1TestRoot.remove();
      ws1Session.save();

      ws2TestRoot.remove();
      ws2Session.save();

      super.tearDown();
   }

   public void testRemoveOneOfTwoVersionable() throws Exception
   {

      ws1Versionable.remove();
      ws1TestRoot.save();

      try
      {
         session.getNodeByUUID(versionHistoryUUID);
      }
      catch (ItemNotFoundException e)
      {
         fail("A version history doesn't exists. " + e);
      }
   }

   private void removeVersionHistory() throws Exception
   {

      ws1Versionable.remove();
      ws1TestRoot.save();

      ws2Versionable.remove();
      ws2TestRoot.save();

      // version history must not exists by now

      // system workspace
      try
      {
         Node vh = session.getNodeByUUID(versionHistoryUUID);
         fail("A version history must not exists but found at " + vh.getPath());
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }

      // ws2
      try
      {
         Node vh = ws2Session.getNodeByUUID(versionHistoryUUID);
         fail("A version history must not exists but is visible from the workspace session "
                  + "where the versionable node was deleted " + ws2Session.getWorkspace().getName() + ". "
                  + vh.getPath());
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }

      // ws1
      try
      {
         Node vh = ws1Session.getNodeByUUID(versionHistoryUUID);
         fail("A version history must not exists but is visible from another workspace session "
                  + ws1Session.getWorkspace().getName() + ". " + vh.getPath());
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }
   }

   public void testRemoveVersionHistory() throws Exception
   {
      removeVersionHistory();
   }

   public void testRemoveVersionHistorySNS() throws Exception
   {

      Node versionable1 = ws1TestRoot.addNode("versionable");
      versionable1.addMixin("mix:versionable");
      Node versionable2 = ws1TestRoot.addNode("versionable");
      versionable2.addMixin("mix:versionable");

      ws1Session.save();
      versionable1.checkin();
      versionable1.checkout();

      versionable2.checkin();
      versionable2.checkout();

      String v1VH = versionable1.getProperty("jcr:versionHistory").getString();
      String v2VH = versionable2.getProperty("jcr:versionHistory").getString();

      try
      {

         versionable1.remove();

         // check versionable2 version history
         // note: versionable2 located at the path of just removed versionable1
         // e.g. /testVHRoot/versionable not a /testVHRoot/versionable[2]
         String v2ReorderedVH = versionable2.getProperty("jcr:versionHistory").getString();
         assertEquals("A version history must be same for auto-reordered node ", v2VH, v2ReorderedVH);

         // a problem there, as the versionable2 was reordered to the versionable1 location
         versionable2.remove();
         ws1Session.save();

         try
         {
            Node vh = session.getNodeByUUID(v1VH);
            fail("A version history must not exists but found at " + vh.getPath());
         }
         catch (ItemNotFoundException e)
         {
            // ok
         }

         try
         {
            Node vh = session.getNodeByUUID(v2VH);
            fail("A version history must not exists but found at " + vh.getPath());
         }
         catch (ItemNotFoundException e)
         {
            // ok
         }
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("A same-name-siblings versionable nodes can't removed well " + e);
      }
   }

   private void prepareChildVersionHistory() throws Exception
   {
      Node nonVersionable = ws1Versionable.addNode("nonVersionable");
      Node ws1VersionableChild = nonVersionable.addNode("versionableChild");
      ws1VersionableChild.addMixin("mix:versionable"); // depended on ws1Versionable's VH
      ws1TestRoot.save();

      ws1VersionableChild.checkin();
      ws1VersionableChild.checkout();

      ws1VersionableChild.addNode("node1");
      ws1TestRoot.save();
      ws1VersionableChild.checkin();
      ws1VersionableChild.checkout();

      ws1Versionable.addNode("nonVersionable2").setProperty("double_prop", 123d);
      ws1Versionable.save();

      Version v1 = ws1Versionable.checkin(); // versionable child in VH of ws1Versionable
      v1.getContainingHistory().addVersionLabel(v1.getName(), "v.with.child", true);
      ws1Versionable.checkout();

      // corresponding node (with common version history)
      // clone to another location
      ws2Session.getWorkspace().clone("ws1", ws1VersionableChild.getPath(),
               ws2TestRoot.getPath() + "/versionableChild", true);

      ws2Versionable.addNode("Dummy node");
      ws2Versionable.save();

      Version v2 = ws2Versionable.checkin();
      v2.getContainingHistory().addVersionLabel(v1.getName(), "v.without.child", true);
      ws2Versionable.checkout();

      Node ws2VersionableChild = ws2TestRoot.getNode("versionableChild");
      ws2VersionableChild.checkin();
      ws2VersionableChild.checkout();

      Node ws2Node1 = ws2VersionableChild.getNode("node1");
      ws2Node1.addNode("node11");
      ws2Node1.setProperty("prop1", Calendar.getInstance());
      ws2VersionableChild.save();

      ws1VersionableChild.checkin();
      ws1VersionableChild.checkout();
   }

   /**
    * Check if remove of versionable node that is contained in child version history will leave this
    * child history because this history is contained in another version history.
    * 
    * @throws Exception
    */
   public void testRemoveChildVersionHistory() throws Exception
   {

      prepareChildVersionHistory();

      // this versioable is contained in ws1Versionable (created at setup)
      Node ws1VersionableChild = ws1TestRoot.getNode("ws1Versionable/nonVersionable/versionableChild");

      // this versioable is NOT contained in ws2Versionable (created at setup)
      Node ws2VersionableChild = ws2TestRoot.getNode("versionableChild");

      assertEquals("Corresponding nodes mut have same uuid", ws1VersionableChild.getUUID(), ws2VersionableChild
               .getUUID());
      assertEquals("Corresponding versionable mut have same history",
               ws1VersionableChild.getVersionHistory().getUUID(), ws2VersionableChild.getVersionHistory().getUUID());

      String childVHUUID = ws1VersionableChild.getVersionHistory().getUUID();

      // we will remove one from ws1
      ws1VersionableChild.remove();
      ws1TestRoot.save();

      // Child VH must exists now
      try
      {
         session.getNodeByUUID(childVHUUID);
      }
      catch (ItemNotFoundException e)
      {
         fail("A version history must exists but it doesn't. UUID: " + childVHUUID);
      }

      // we will remove last versionable (of child VH) from ws2
      ws2VersionableChild.remove();
      ws2TestRoot.save();

      // Child VH must exists now too
      try
      {
         session.getNodeByUUID(childVHUUID);
      }
      catch (ItemNotFoundException e)
      {
         fail("A version history must exists but it doesn't. UUID: " + childVHUUID);
      }

      // we will remove versionable node which VH contains this child history
      removeVersionHistory();

      // and child version history must not exists too
      try
      {
         Node vh = session.getNodeByUUID(childVHUUID);
         fail("A child version history must not exists after the parent history remove but found at " + vh.getPath());
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }
   }
}
