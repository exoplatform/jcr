/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.lab.cluster.prepare;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestQueryPrepare.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestQueryPrepare extends JcrAPIBaseTest
{
   public void testPrepareNodes() throws RepositoryException, IOException
   {
      System.out.println("preparing....");
      
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      Node doc2 = root.addNode("document2", "nt:file");
      cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      session.save();
      System.out.println("Done!");
      try
      {
         Thread.sleep(60000);
      }
      catch (InterruptedException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      // do noting
   }
}
