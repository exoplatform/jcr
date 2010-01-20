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
package org.exoplatform.services.jcr.lab.cluster.prepare;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestFullSearchPrepare.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestFullSearchPrepare extends JcrAPIBaseTest
{

   public void testFullSearch() throws RepositoryException
   {
      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont1 = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont1.setProperty("jcr:mimeType", "text/plain");
      cont1.setProperty("jcr:lastModified", Calendar.getInstance());
      cont1.setProperty("jcr:data", "The quick brown fox jump over the lazy dog");
      session.save();

      Node doc2 = root.addNode("document2", "nt:file");
      NodeImpl cont2 = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont2.setProperty("jcr:mimeType", "text/plain");
      cont2.setProperty("jcr:lastModified", Calendar.getInstance());
      cont2.setProperty("jcr:data", "Dogs do not like cats.");

      Node doc3 = root.addNode("document3", "nt:file");
      NodeImpl cont3 = (NodeImpl)doc3.addNode("jcr:content", "nt:resource");
      cont3.setProperty("jcr:mimeType", "text/plain");
      cont3.setProperty("jcr:lastModified", Calendar.getInstance());
      cont3.setProperty("jcr:data", "Cats jumping high.");
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
