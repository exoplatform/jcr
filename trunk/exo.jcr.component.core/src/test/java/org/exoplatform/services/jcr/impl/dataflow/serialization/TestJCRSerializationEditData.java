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

import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 16.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: TestJCRSerializationEditDataTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestJCRSerializationEditData extends JcrImplSerializationBaseTest
{

   public void testAddNode() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      Node test = session.getRootNode().addNode("cms3").addNode("test");

      Node cool = test.addNode("nnn", "nt:file");
      Node contentNode = cool.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", "_______________simple data________________");
      contentNode.setProperty("jcr:mimeType", "plain/text");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      session.save();
      
      // check 1
      List<TransactionChangesLog> srcLog = pl.pushChanges();

      File jcrfile = super.serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
      
      // edit 1
      pl = new TesterItemsPersistenceListener(this.session);
      String newData = "____________simple_data_2____________";
      session.getRootNode().getNode("cms3").getNode("test").getNode("nnn").getNode("jcr:content").setProperty(
         "jcr:data", newData);
      session.save();

      // check 1
      srcLog = pl.pushChanges();

      jcrfile = super.serializeLogs(srcLog);

      destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
      

      
   }
}
