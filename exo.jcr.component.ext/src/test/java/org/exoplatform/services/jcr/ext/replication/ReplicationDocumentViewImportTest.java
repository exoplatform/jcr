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
package org.exoplatform.services.jcr.ext.replication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 21.08.2009 10:59:36
 * 
 * @version $Id: ReplicationDocumentViewImportTest.java 25.08.2009 10:49:36 rainfox
 */

public class ReplicationDocumentViewImportTest
   extends BaseReplicationTest
{
   public void testDocumentViewImportOverSession() throws Exception
   {
      Node nl1 = root.addNode("s_doc_view_node_l1");
      nl1.addNode("s_doc_view_node_l2");
      root.save();

      File f = File.createTempFile("s_doc_view", ".xml");
      f.deleteOnExit();

      // export
      session.exportDocumentView(nl1.getPath(), new FileOutputStream(f), false, false);

      Node parentImportPath = root.addNode("s_imported_doc_view_node");
      root.save();

      //import
      session.importXML(parentImportPath.getPath(), new FileInputStream(f), 0);
      session.save();

      Thread.sleep(5 * 1000);

      // check
      root.getNode("s_imported_doc_view_node").getNode("s_doc_view_node_l1").getNode("s_doc_view_node_l2");
      root2.getNode("s_imported_doc_view_node").getNode("s_doc_view_node_l1").getNode("s_doc_view_node_l2");
   }

   public void testDocumentViewImportOverWorksapce() throws Exception
   {
      Node nl1 = root.addNode("w_doc_view_node_l1");
      nl1.addNode("w_doc_view_node_l2");
      root.save();

      File f = File.createTempFile("w_doc_view", ".xml");
      f.deleteOnExit();

      // export
      session.exportDocumentView(nl1.getPath(), new FileOutputStream(f), false, false);

      Node parentImportPath = root.addNode("w_imported_doc_view_node");
      root.save();

      //import
      session.getWorkspace().importXML(parentImportPath.getPath(), new FileInputStream(f), 0);
      session.save();

      Thread.sleep(5 * 1000);

      // check
      root.getNode("w_imported_doc_view_node").getNode("w_doc_view_node_l1").getNode("w_doc_view_node_l2");
      root2.getNode("w_imported_doc_view_node").getNode("w_doc_view_node_l1").getNode("w_doc_view_node_l2");
   }
}
