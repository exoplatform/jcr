/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.api.importing;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestImportImage.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestImportVersionedChild extends JcrAPIBaseTest
{

   Node testRoot;

   public void setUp() throws Exception
   {
      super.setUp();
      testRoot = this.root.addNode("parent", "nt:folder");
      root.save();
   }

   public void tearDown() throws Exception
   {
      testRoot.remove();
      root.save();
      super.tearDown();
   }

   protected void loadTestTree() throws Exception
   {
      // wc1/medias/picture
      Node wc1 = testRoot.addNode("wc1", "nt:folder");
      wc1.addMixin("mix:versionable");
      testRoot.save();
      Node medias = wc1.addNode("medias", "nt:folder");

      Node picture = medias.addNode("picture", "nt:file");
      picture.addMixin("mix:versionable");

      Node res = picture.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("bla bla".getBytes()));
      MimeTypeResolver mimres = new MimeTypeResolver();
      res.setProperty("jcr:mimeType", mimres.getMimeType("screen.txt"));
      root.save();
   }

   public void testImportVersionHistoryPreloadChildVersionHistory() throws Exception
   {
      loadTestTree();
      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");
      Node picture = (NodeImpl)session.getItem("/parent/wc1/medias/picture");

      // make checkin/checkout a lot

      wc1.checkin();
      wc1.checkout();

      picture.checkin();
      picture.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false);

      ByteArrayOutputStream childvhout = new ByteArrayOutputStream();
      session.exportSystemView(picture.getVersionHistory().getPath(), childvhout, false, false);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false);

      // prepare data for version import

      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // prepare data for child version import

      String chversionHistory = picture.getProperty("jcr:versionHistory").getValue().getString();
      String chbaseVersion = picture.getProperty("jcr:baseVersion").getValue().getString();
      Value[] chjcrPredecessors = picture.getProperty("jcr:predecessors").getValues();
      StringBuilder chjcrPredecessorsBuilder = new StringBuilder();
      String[] chpredecessorsHistory;
      for (Value value : chjcrPredecessors)
      {
         if (chjcrPredecessorsBuilder.length() > 0)
            chjcrPredecessorsBuilder.append(",");
         chjcrPredecessorsBuilder.append(value.getString());
      }
      if (chjcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         chpredecessorsHistory = chjcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         chpredecessorsHistory = new String[]{chjcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");
      picture = wc1.getNode("medias").getNode("picture");

      VersionHistoryImporter chversionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)picture, new ByteArrayInputStream(childvhout.toByteArray()),
            chbaseVersion, chpredecessorsHistory, chversionHistory);
      chversionHistoryImporter.doImport();
      session.save();

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      assertTrue(picture.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, picture.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, picture.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], picture.getProperty("jcr:predecessors").getValues()[0].getString());
   }

   public void testImportVersionHistory() throws Exception
   {
      loadTestTree();
      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");

      // make checkin/checkout 
      wc1.checkin();
      wc1.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false);

      // prepare data for version import
      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      Node picture = wc1.getNode("medias").getNode("picture");
      assertTrue(picture.isNodeType("mix:versionable"));

      //try to remove wc1, there must be RepositoryException
      try
      {
         wc1.remove();
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // OK - wc1  Version History contain nt:versionedChild with link to non exist Version history

         // remove bugy version history
         SessionDataManager dataManager = session.getTransientNodesManager();
         NodeImpl vhPicture =
            (NodeImpl)session.getItem("/jcr:system/jcr:versionStorage/" + versionHistory
               + "/1/jcr:frozenNode/medias/picture");

         assertTrue(vhPicture.isNodeType("nt:versionedChild"));

         PlainChangesLog changesLogDelete = new PlainChangesLogImpl();
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)vhPicture.getProperty("jcr:primaryType"))
            .getData()));
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)vhPicture
            .getProperty("jcr:childVersionHistory")).getData()));
         for (ItemState itemState : changesLogDelete.getAllStates())
         {
            dataManager.delete(itemState.getData(), itemState.getAncestorToSave());
         }
         session.save();
      }
   }

   public void testImportVersionHistoryWithChildVersions() throws Exception
   {
      loadTestTree();
      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");

      // make checkin/checkout a lot
      wc1.checkin();
      wc1.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false, true);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

      // prepare data for version import
      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // remember "picture" nodes version history data
      Node picture = (NodeImpl)session.getItem("/parent/wc1/medias/picture");
      String chversionHistory = picture.getProperty("jcr:versionHistory").getValue().getString();
      String chbaseVersion = picture.getProperty("jcr:baseVersion").getValue().getString();
      Value[] chjcrPredecessors = picture.getProperty("jcr:predecessors").getValues();
      StringBuilder chjcrPredecessorsBuilder = new StringBuilder();
      String[] chpredecessorsHistory;
      for (Value value : chjcrPredecessors)
      {
         if (chjcrPredecessorsBuilder.length() > 0)
            chjcrPredecessorsBuilder.append(",");
         chjcrPredecessorsBuilder.append(value.getString());
      }
      if (chjcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         chpredecessorsHistory = chjcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         chpredecessorsHistory = new String[]{chjcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      picture = wc1.getNode("medias").getNode("picture");
      assertTrue(picture.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, picture.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, picture.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], picture.getProperty("jcr:predecessors").getValues()[0].getString());
   }

   public void testImportVersionHistoryPreloadChildVersionHistoryWithChildVersions() throws Exception
   {
      loadTestTree();
      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");
      Node picture = (NodeImpl)session.getItem("/parent/wc1/medias/picture");

      // make checkin/checkout a lot

      wc1.checkin();
      wc1.checkout();

      picture.checkin();
      picture.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false);

      ByteArrayOutputStream childvhout = new ByteArrayOutputStream();
      session.exportSystemView(picture.getVersionHistory().getPath(), childvhout, false, false, true);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

      // prepare data for version import

      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // prepare data for child version import

      String chversionHistory = picture.getProperty("jcr:versionHistory").getValue().getString();
      String chbaseVersion = picture.getProperty("jcr:baseVersion").getValue().getString();
      Value[] chjcrPredecessors = picture.getProperty("jcr:predecessors").getValues();
      StringBuilder chjcrPredecessorsBuilder = new StringBuilder();
      String[] chpredecessorsHistory;
      for (Value value : chjcrPredecessors)
      {
         if (chjcrPredecessorsBuilder.length() > 0)
            chjcrPredecessorsBuilder.append(",");
         chjcrPredecessorsBuilder.append(value.getString());
      }
      if (chjcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         chpredecessorsHistory = chjcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         chpredecessorsHistory = new String[]{chjcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");
      picture = wc1.getNode("medias").getNode("picture");

      VersionHistoryImporter chversionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)picture, new ByteArrayInputStream(childvhout.toByteArray()),
            chbaseVersion, chpredecessorsHistory, chversionHistory);
      chversionHistoryImporter.doImport();
      session.save();

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      assertTrue(picture.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, picture.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, picture.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], picture.getProperty("jcr:predecessors").getValues()[0].getString());
   }

   /**
    * Many mix:versionable subnodes. 
    * @throws Exception
    */
   public void testImportVersionHistoryWithManySubversions() throws Exception
   {

      // wc1/medias/picture
      Node wc1 = testRoot.addNode("wc1", "nt:folder");
      wc1.addMixin("mix:versionable");
      testRoot.save();
      Node medias = wc1.addNode("medias", "nt:folder");

      Node picture = medias.addNode("picture", "nt:file");
      picture.addMixin("mix:versionable");

      Node res = picture.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("bla bla".getBytes()));
      MimeTypeResolver mimres = new MimeTypeResolver();
      res.setProperty("jcr:mimeType", mimres.getMimeType("screen.txt"));
      root.save();

      Node subNode1 = medias.addNode("subnode1", "nt:folder");
      subNode1.addMixin("mix:versionable");
      root.save();

      Node subNode2 = subNode1.addNode("subnode2", "nt:folder");
      Node subNode3 = subNode2.addNode("subnode3", "nt:folder");
      subNode3.addMixin("mix:versionable");
      root.save();

      Node subNode4 = subNode3.addNode("subnode4", "nt:folder");
      subNode4.addMixin("mix:versionable");
      root.save();

      // /medias/subnode1/subnode2/subnode3/subnode4
      wc1.checkin();
      wc1.checkout();

      picture.checkin();
      picture.checkout();

      subNode1.checkin();
      subNode1.checkout();

      subNode4.checkin();
      subNode4.checkout();

      subNode3.checkin();
      subNode3.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false, true);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

      // prepare data for version import

      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // prepare data for child version import

      String chversionHistory = subNode4.getProperty("jcr:versionHistory").getValue().getString();
      String chbaseVersion = subNode4.getProperty("jcr:baseVersion").getValue().getString();
      Value[] chjcrPredecessors = subNode4.getProperty("jcr:predecessors").getValues();
      StringBuilder chjcrPredecessorsBuilder = new StringBuilder();
      String[] chpredecessorsHistory;
      for (Value value : chjcrPredecessors)
      {
         if (chjcrPredecessorsBuilder.length() > 0)
            chjcrPredecessorsBuilder.append(",");
         chjcrPredecessorsBuilder.append(value.getString());
      }
      if (chjcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         chpredecessorsHistory = chjcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         chpredecessorsHistory = new String[]{chjcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");

      subNode4 = (NodeImpl)session.getItem("/parent/wc1/medias/subnode1/subnode2/subnode3/subnode4");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      assertTrue(subNode4.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, subNode4.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, subNode4.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], subNode4.getProperty("jcr:predecessors").getValues()[0].getString());
   }

   public void testImportVersionHistoryManyVersions() throws Exception
   {
      // wc1/medias/picture
      Node wc1 = testRoot.addNode("wc1", "nt:folder");
      wc1.addMixin("mix:versionable");
      testRoot.save();
      Node medias = wc1.addNode("medias", "nt:folder");

      Node picture = medias.addNode("picture", "nt:file");
      picture.addMixin("mix:versionable");

      Node res = picture.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("bla bla".getBytes()));
      MimeTypeResolver mimres = new MimeTypeResolver();
      res.setProperty("jcr:mimeType", mimres.getMimeType("screen.txt"));
      root.save();

      // make checkin/checkout a lot
      wc1.checkin();
      wc1.checkout();

      picture.checkin();
      picture.checkout();

      res.setProperty("jcr:data", new ByteArrayInputStream("new data".getBytes()));
      root.save();

      picture.checkin();
      picture.checkout();

      // check before import

      picture.restore("1", true);
      String strvalue = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("bla bla", strvalue);

      picture.restore("2", true);
      strvalue = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("new data", strvalue);

      picture.checkout();

      // make new version
      assertTrue(picture.getProperty("jcr:isCheckedOut").getValue().getBoolean());

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false, true);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

      // prepare data for version import

      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      String childVersionHistory = picture.getProperty("jcr:versionHistory").getValue().getString();
      String childBaseVersion = picture.getProperty("jcr:baseVersion").getValue().getString();
      Value[] childJcrPredecessors = picture.getProperty("jcr:predecessors").getValues();
      StringBuilder childJcrPredecessorsBuilder = new StringBuilder();
      String[] childPredecessorsHistory;
      for (Value value : childJcrPredecessors)
      {
         if (childJcrPredecessorsBuilder.length() > 0)
            childJcrPredecessorsBuilder.append(",");
         childJcrPredecessorsBuilder.append(value.getString());
      }
      if (childJcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         childPredecessorsHistory = childJcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         childPredecessorsHistory = new String[]{childJcrPredecessorsBuilder.toString()};
      }

      String chversionHistory = picture.getProperty("jcr:versionHistory").getValue().getString();
      String chbaseVersion = picture.getProperty("jcr:baseVersion").getValue().getString();
      Value[] chjcrPredecessors = picture.getProperty("jcr:predecessors").getValues();
      StringBuilder chjcrPredecessorsBuilder = new StringBuilder();
      String[] chpredecessorsHistory;
      for (Value value : chjcrPredecessors)
      {
         if (chjcrPredecessorsBuilder.length() > 0)
            chjcrPredecessorsBuilder.append(",");
         chjcrPredecessorsBuilder.append(value.getString());
      }
      if (chjcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         chpredecessorsHistory = chjcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         chpredecessorsHistory = new String[]{chjcrPredecessorsBuilder.toString()};
      }

      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      picture = wc1.getNode("medias").getNode("picture");
      assertTrue(picture.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, picture.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, picture.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], picture.getProperty("jcr:predecessors").getValues()[0].getString());
      assertTrue(picture.getProperty("jcr:isCheckedOut").getValue().getBoolean());

      String value = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("new data", value);

      picture.restore("1", true);
      value = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("bla bla", value);

      picture.restore("2", true);
      value = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("new data", value);

      picture.checkout();

      // make new version
      assertTrue(picture.getProperty("jcr:isCheckedOut").getValue().getBoolean());
      res = picture.getNode("jcr:content");
      res.setProperty("jcr:data", new ByteArrayInputStream("third".getBytes()));
      root.save();

      picture.checkin();
      picture.checkout();

      picture.restore("1", true);
      value = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("bla bla", value);

      picture.restore("3", true);
      value = picture.getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals("third", value);
   }

   /**
    * Many mix:versionable subnodes. 
    * @throws Exception
    */
   public void testImportVersionHistoryWithManyVersions() throws Exception
   {

      // wc1/medias/picture
      Node wc1 = testRoot.addNode("wc1", "nt:folder");
      wc1.addMixin("mix:versionable");
      testRoot.save();
      Node medias = wc1.addNode("medias", "nt:folder");

      Node picture = medias.addNode("picture", "nt:file");
      picture.addMixin("mix:versionable");

      Node res = picture.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("bla bla".getBytes()));
      MimeTypeResolver mimres = new MimeTypeResolver();
      res.setProperty("jcr:mimeType", mimres.getMimeType("screen.txt"));
      root.save();

      wc1.checkin();
      wc1.checkout();

      wc1.checkin();
      wc1.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/wc1", out, false, false, true);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

      // prepare data for version import
      String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      String[] predecessorsHistory;
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }
      // remove node
      wc1.remove();
      session.save();

      out.close();
      vhout.close();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      wc1 = (NodeImpl)session.getItem("/parent/wc1");
      picture = wc1.getNode("medias").getNode("picture");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      assertTrue(picture.isNodeType("mix:versionable"));
   }

   public void testImportVersionHistoryNonSysWorkspace() throws Exception
   {

      SessionImpl session = (SessionImpl)this.repository.login(credentials, "ws1");

      Node root = session.getRootNode();
      Node testRoot = root.addNode("parent", "nt:folder");
      root.save();

      try
      {
         // wc1/medias/picture
         Node wc1 = testRoot.addNode("wc1", "nt:folder");
         wc1.addMixin("mix:versionable");
         testRoot.save();
         Node medias = wc1.addNode("medias", "nt:folder");

         Node picture = medias.addNode("picture", "nt:file");
         picture.addMixin("mix:versionable");

         Node res = picture.addNode("jcr:content", "nt:resource");
         res.setProperty("jcr:lastModified", Calendar.getInstance());
         res.setProperty("jcr:data", new ByteArrayInputStream("bla bla".getBytes()));
         MimeTypeResolver mimres = new MimeTypeResolver();
         res.setProperty("jcr:mimeType", mimres.getMimeType("screen.txt"));
         root.save();

         // make checkin/checkout a lot

         wc1.checkin();
         wc1.checkout();

         // export import version history and node
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         session.exportSystemView("/parent/wc1", out, false, false, true);

         ByteArrayOutputStream vhout = new ByteArrayOutputStream();
         session.exportSystemView(wc1.getVersionHistory().getPath(), vhout, false, false, true);

         // prepare data for version import

         String versionHistory = wc1.getProperty("jcr:versionHistory").getValue().getString();
         String baseVersion = wc1.getProperty("jcr:baseVersion").getValue().getString();
         Value[] jcrPredecessors = wc1.getProperty("jcr:predecessors").getValues();
         StringBuilder jcrPredecessorsBuilder = new StringBuilder();
         String[] predecessorsHistory;
         for (Value value : jcrPredecessors)
         {
            if (jcrPredecessorsBuilder.length() > 0)
               jcrPredecessorsBuilder.append(",");
            jcrPredecessorsBuilder.append(value.getString());
         }
         if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
         {
            predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
         }
         else
         {
            predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
         }

         // remove node
         wc1.remove();
         session.save();

         out.close();
         vhout.close();

         // import
         session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

         session.save();

         wc1 = (NodeImpl)session.getItem("/parent/wc1");

         VersionHistoryImporter versionHistoryImporter =
            new VersionHistoryImporter((NodeImpl)wc1, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
               predecessorsHistory, versionHistory);
         versionHistoryImporter.doImport();
         session.save();

         picture = wc1.getNode("medias").getNode("picture");
         assertTrue(picture.isNodeType("mix:versionable"));
      }
      finally
      {
         testRoot.remove();
         root.save();
         session.logout();
      }
   }

   public void testImportVersionHistoryFromFileWithChildVH() throws Exception
   {

      String baseVersion = "397dad17c0a8004201c7b45ea76d4b1b";
      String[] predecessorsHistory = new String[]{"397dad17c0a8004201c7b45ea76d4b1b"};
      String versionHistory = "397dac8bc0a8004201729d052a305832";

      String chbaseVersion = "397dac9ac0a8004201cde5722fec978e";
      String[] chpredecessorsHistory = new String[]{"397dac9ac0a8004201cde5722fec978e"};
      String chversionHistory = "397dac9ac0a8004200f37de3ace7b0ad";

      InputStream is =
         TestImportVersionedChild.class.getResourceAsStream("/import-export/data_with_versioned_child.xml");
      InputStream vhis =
         TestImportVersionedChild.class.getResourceAsStream("/import-export/vh_with_versioned_child.xml");

      // import
      session.importXML("/parent", is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, vhis, baseVersion, predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      Node picture = wc1.getNode("medias").getNode("picture");
      assertTrue(picture.isNodeType("mix:versionable"));
      assertEquals(chversionHistory, picture.getProperty("jcr:versionHistory").getValue().getString());
      assertEquals(chbaseVersion, picture.getProperty("jcr:baseVersion").getValue().getString());
      assertEquals(chpredecessorsHistory[0], picture.getProperty("jcr:predecessors").getValues()[0].getString());

   }

   public void testImportVersionHistoryFromFile() throws Exception
   {

      String baseVersion = "0019980ec0a80042014313ff82e97096";
      String[] predecessorsHistory = new String[]{"0019980ec0a80042014313ff82e97096"};
      String versionHistory = "001997a1c0a80042007d98739b97e1bc";

      String chbaseVersion = "001997b1c0a8004200d65c82779a2e13";
      String[] chpredecessorsHistory = new String[]{"001997b1c0a8004200d65c82779a2e13"};
      String chversionHistory = "001997b1c0a8004201d35a6fa36ef4e7";

      InputStream is = TestImportVersionedChild.class.getResourceAsStream("/import-export/data.xml");
      InputStream vhis = TestImportVersionedChild.class.getResourceAsStream("/import-export/vh.xml");

      // import
      session.importXML("/parent", is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      Node wc1 = (NodeImpl)session.getItem("/parent/wc1");

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)wc1, vhis, baseVersion, predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      Node picture = wc1.getNode("medias").getNode("picture");
      assertTrue(picture.isNodeType("mix:versionable"));
      assertFalse(chversionHistory.equals(picture.getProperty("jcr:versionHistory").getValue().getString()));
      assertFalse(chbaseVersion.equals(picture.getProperty("jcr:baseVersion").getValue().getString()));
      assertFalse(chpredecessorsHistory[0].equals(picture.getProperty("jcr:predecessors").getValues()[0].getString()));

      // try to remove picture, there must be RepositoryException
      try
      {
         wc1.remove();
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // OK - wc1  Version History contain nt:versionedChild with link to non exist Version history

         // remove bugy version history
         SessionDataManager dataManager = session.getTransientNodesManager();
         NodeImpl vhPicture =
            (NodeImpl)session.getItem("/jcr:system/jcr:versionStorage/" + versionHistory
               + "/1/jcr:frozenNode/medias/picture");

         assertTrue(vhPicture.isNodeType("nt:versionedChild"));

         PlainChangesLog changesLogDelete = new PlainChangesLogImpl();
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)vhPicture.getProperty("jcr:primaryType"))
            .getData()));
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)vhPicture
            .getProperty("jcr:childVersionHistory")).getData()));

         for (ItemState itemState : changesLogDelete.getAllStates())
         {
            dataManager.delete(itemState.getData(), itemState.getAncestorToSave());
         }

         session.save();
      }
   }

}
