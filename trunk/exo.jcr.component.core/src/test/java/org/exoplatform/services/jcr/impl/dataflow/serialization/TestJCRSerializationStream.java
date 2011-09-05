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
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 16.02.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestJCRSerializationStream.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestJCRSerializationStream extends JcrImplSerializationBaseTest
{

   public void testAddStreamData() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      File tempFile = this.createBLOBTempFile(50000);

      Node test = root.addNode("cms2").addNode("test");
      Node cool = test.addNode("nnn", "nt:file");
      Node contentNode = cool.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      session.save();
      checkResults(pl.getAndReset());

      test.setProperty("creator", new String[]{"Creator 1", "Creator 2", "Creator 3"});

      ValueFactory vf = cool.getSession().getValueFactory();
      test.setProperty("date", new Value[]{vf.createValue(Calendar.getInstance()),
         vf.createValue(Calendar.getInstance()), vf.createValue(Calendar.getInstance())});

      test.setProperty("source", new String[]{"Source 1", "Source 2", "Source 3"});
      test.setProperty("description", new String[]{"description 1", "description 2", "description 3", "description 4"});
      test.setProperty("publisher", new String[]{"publisher 1", "publisher 2", "publisher 3"});
      test.setProperty("language", new String[]{"language 1", "language 2", "language3", "language 4", "language5"});

      session.save();
      checkResults(pl.getAndReset());

      // delete
      Node srcParent = test.getParent();
      srcParent.remove();
      session.save();

      checkResults(pl.getAndReset());
      // unregister listener
      pl.pushChanges();
   }
}
