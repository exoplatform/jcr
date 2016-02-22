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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 16.02.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestJCRSerializationVersionRestore.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestJCRSerializationVersionRestore extends JcrImplSerializationBaseTest
{

   public void testRestore() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      Node srcVersionNode = session.getRootNode().addNode("Version node 1");
      srcVersionNode.setProperty("jcr:data", "Base version");
      srcVersionNode.addMixin("mix:versionable");
      session.save();

      checkResults(pl.getAndReset());

      srcVersionNode.checkin();
      session.save();

      checkResults(pl.getAndReset());

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 1");
      session.save();

      checkResults(pl.getAndReset());

      srcVersionNode.checkin();
      session.save();

      checkResults(pl.getAndReset());

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 2");
      session.save();

      checkResults(pl.getAndReset());

      Version baseVersion = srcVersionNode.getBaseVersion();
      srcVersionNode.restore(baseVersion, true);
      session.save();

      checkResults(pl.getAndReset());

      Version baseVersion1 = srcVersionNode.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersionNode.restore(restoreToBaseVersion, true);
      session.save();

      checkResults(pl.getAndReset());
      // unregister listener
      pl.pushChanges();
   }

   public void testBigFileRestore() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);
      //List<TransactionChangesLog> srcLog = new ArrayList<TransactionChangesLog>();

      File tempFile = File.createTempFile("tempFile", "doc");
      File tempFile2 = File.createTempFile("tempFile", "doc");
      File tempFile3 = File.createTempFile("tempFile", "doc");
      tempFile.deleteOnExit();
      tempFile2.deleteOnExit();
      tempFile3.deleteOnExit();

      FileOutputStream fos = new FileOutputStream(tempFile);
      FileOutputStream fos2 = new FileOutputStream(tempFile2);
      FileOutputStream fos3 = new FileOutputStream(tempFile3);

      String content = "this is the content #1";
      String content2 = "this is the content #2_";
      String content3 = "this is the content #3__";

      for (int i = 0; i < 15000; i++)
      {
         fos.write((i + " " + content).getBytes());
         fos2.write((i + " " + content2).getBytes());
         fos3.write((i + " " + content3).getBytes());
      }

      fos.close();
      fos2.close();
      fos3.close();

      Node srcVersionNode = root.addNode("nt_file_node", "nt:file");
      Node contentNode = srcVersionNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      srcVersionNode.addMixin("mix:versionable");

      session.save();

      checkResults(pl.getAndReset());

      Node srcVersion = root.getNode("nt_file_node");
      srcVersion.checkin();
      session.save();

      checkResults(pl.getAndReset());

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile2));
      session.save();

      checkResults(pl.getAndReset());

      srcVersion.checkin();
      session.save();

      checkResults(pl.getAndReset());

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile3));
      session.save();

      checkResults(pl.getAndReset());

      Version baseVersion = srcVersion.getBaseVersion();
      srcVersion.restore(baseVersion, true);
      session.save();

      checkResults(pl.getAndReset());

      Version baseVersion1 = srcVersion.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersion.restore(restoreToBaseVersion, true);
      session.save();

      checkResults(pl.getAndReset());

      Version baseVersion2 = srcVersion.getBaseVersion();
      Version[] predesessors2 = baseVersion2.getSuccessors();
      Version restoreToBaseVersion_2 = predesessors2[0];

      srcVersion.restore(restoreToBaseVersion_2, true);
      session.save();

      checkResults(pl.getAndReset());
      // unregister listener
      pl.pushChanges();
   }

}
