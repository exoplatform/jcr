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

import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 16.02.2009
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

      srcVersionNode.checkin();
      session.save();

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 1");
      session.save();

      srcVersionNode.checkin();
      session.save();

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 2");
      session.save();

      Version baseVersion = srcVersionNode.getBaseVersion();
      srcVersionNode.restore(baseVersion, true);
      session.save();

      Version baseVersion1 = srcVersionNode.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersionNode.restore(restoreToBaseVersion, true);
      session.save();

      List<TransactionChangesLog> srcLog = pl.pushChanges();

      File jcrfile = super.serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
   }

   public void testBigFileRestore() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      File tempFile = File.createTempFile("tempFile", "doc");
      File tempFile2 = File.createTempFile("tempFile", "doc");
      File tempFile3 = File.createTempFile("tempFile", "doc");
      tempFile.deleteOnExit();
      tempFile2.deleteOnExit();
      tempFile3.deleteOnExit();

      FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);
      FileOutputStream fos2 = PrivilegedFileHelper.fileOutputStream(tempFile2);
      FileOutputStream fos3 = PrivilegedFileHelper.fileOutputStream(tempFile3);

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

      log.info("FILE for VERVION #1 : file size = " + tempFile.length() + " bytes");
      log.info("FILE for VERVION #2 : file size = " + tempFile2.length() + " bytes");
      log.info("FILE for VERVION #3 : file size = " + tempFile3.length() + " bytes");

      Node srcVersionNode = root.addNode("nt_file_node", "nt:file");
      Node contentNode = srcVersionNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      srcVersionNode.addMixin("mix:versionable");

      session.save();

      Node srcVersion = root.getNode("nt_file_node");
      srcVersion.checkin();
      session.save();

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile2));
      session.save();

      srcVersion.checkin();
      session.save();

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile3));
      session.save();

      Version baseVersion = srcVersion.getBaseVersion();
      srcVersion.restore(baseVersion, true);
      session.save();

      Version baseVersion1 = srcVersion.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersion.restore(restoreToBaseVersion, true);
      session.save();

      Version baseVersion2 = srcVersion.getBaseVersion();
      Version[] predesessors2 = baseVersion2.getSuccessors();
      Version restoreToBaseVersion_2 = predesessors2[0];

      srcVersion.restore(restoreToBaseVersion_2, true);
      session.save();

      List<TransactionChangesLog> srcLog = pl.pushChanges();

      File jcrfile = super.serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
   }

}
