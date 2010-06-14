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
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TetsPropsDeserialization.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestPropsDeserialization extends JcrImplSerializationBaseTest
{

   public void testPropReSetVal() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      File content1 = this.createBLOBTempFile(300);
      File content2 = this.createBLOBTempFile(301);

      Node srcVersionNode = root.addNode("nt_file_node", "nt:file");
      Node contentNode = srcVersionNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(content1));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      srcVersionNode.addMixin("mix:versionable");

      session.save();

      // check 1
      List<TransactionChangesLog> logs = pl.pushChanges();

      File jcrfile = super.serializeLogs(logs);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(logs.size(), destLog.size());

      for (int i = 0; i < logs.size(); i++)
         checkIterator(logs.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());

      // set value
      pl = new TesterItemsPersistenceListener(this.session);
      contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(content2));
      session.save();

      // check 2
      logs = pl.pushChanges();

      jcrfile = super.serializeLogs(logs);

      destLog = super.deSerializeLogs(jcrfile);

      assertEquals(logs.size(), destLog.size());

      for (int i = 0; i < logs.size(); i++)
         checkIterator(logs.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
   }

}
