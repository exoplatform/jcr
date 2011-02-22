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
import org.exoplatform.services.jcr.util.VersionHistoryImporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
}
