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
package org.exoplatform.services.jcr.usecases.index;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:zagrebin_v@mail.ru">Victor Zagrebin</a>
 * @version $Id: SlowListTest.java 11907 2008-03-13 15:36:21Z ksm $ The test for indexing an excel
 *          .xls file which contained within jcr:data property
 */

public class SlowListTest extends BaseUsecasesTest
{

   /**
    * The test for indexing an excel .xls file
    * 
    * @throws Exception
    */
   public void testIndexTextPlainFile() throws Exception
   {
      // variables for the execution time
      long start, end;
      InputStream is = SlowListTest.class.getResourceAsStream("/index/test_index.xls");
      assertNotNull("Can not create an input stream from file for indexing", is);

      Node test = root.addNode("cms2").addNode("test");
      start = System.currentTimeMillis(); // to get the time of start
      assertNotNull("Can not create a test node for indexing", test);
      for (int i = 0; i < 111; i++)
      {
         is = SlowListTest.class.getResourceAsStream("/index/test_index.xls");
         String name = new String("nnn-" + i);
         Node cool = test.addNode(name, "nt:file");
         Node contentNode = cool.addNode("jcr:content", "nt:resource");
         //contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", is);
         contentNode.setProperty("jcr:mimeType", "application/excel");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
         assertNotNull("Can not create a cool node for indexing", cool);
         assertNotNull("Can not create a contentNode node for indexing", contentNode);
      }
      end = System.currentTimeMillis();
      session.save();
      session.save();
      start = System.currentTimeMillis();
      Query q;
      String xpath = "/jcr:root/cms2/test//*";

      q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
      assertNotNull("Can not create a query for indexing", q);
      QueryResult result = q.execute();
      end = System.currentTimeMillis();

      start = System.currentTimeMillis();
      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
      }
      end = System.currentTimeMillis();

      start = System.currentTimeMillis();
      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
      }
      end = System.currentTimeMillis();

      // -------------------------------------------------------------------------------------
      Node n2 = test.addNode("fff");
      session.save();
      result = q.execute();
      start = System.currentTimeMillis();
      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
      }
      end = System.currentTimeMillis();

      start = System.currentTimeMillis();
      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
      }
      end = System.currentTimeMillis();

      // [PN] 21.07.06 hasn't fails that no fail
      // fail("QUERY TEST"); // Only for the view of intermediate results
   }
}
