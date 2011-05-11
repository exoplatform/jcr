/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class TestImportVersionHistory extends JcrAPIBaseTest
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

   public void testImportVersionHistory() throws Exception
   {

      Node parent = testRoot.addNode("child", "nt:folder");
      parent.addMixin("mix:versionable");

      Node subNode = parent.addNode("subnode", "nt:file");
      subNode.addMixin("mix:referenceable");

      Node res = subNode.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      res.setProperty("jcr:mimeType", "application/x-javascript");

      root.save();

      // make checkin/checkout

      parent.checkin();
      parent.checkout();

      subNode.remove();
      subNode = parent.addNode("subnode", "nt:file");
      subNode.addMixin("mix:referenceable");

      res = subNode.addNode("jcr:content", "nt:resource");
      res.setProperty("jcr:lastModified", Calendar.getInstance());
      res.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      res.setProperty("jcr:mimeType", "application/x-javascript");
      root.save();

      parent.checkin();
      parent.checkout();

      // export import version history and node
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      session.exportSystemView("/parent/child", out, false, false);

      ByteArrayOutputStream vhout = new ByteArrayOutputStream();
      session.exportSystemView(parent.getVersionHistory().getPath(), vhout, false, false);

      // prepare data for version import

      String versionHistory = parent.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = parent.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = parent.getProperty("jcr:predecessors").getValues();
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
      parent.remove();
      session.save();

      // import
      session.importXML("/parent", new ByteArrayInputStream(out.toByteArray()),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true);

      session.save();

      parent = (NodeImpl)session.getItem("/parent/child");
      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)parent, new ByteArrayInputStream(vhout.toByteArray()), baseVersion,
            predecessorsHistory, versionHistory);
      versionHistoryImporter.doImport();
      session.save();

      // try to restore first version
      parent.restore("1", true);
   }
}
