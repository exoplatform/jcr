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

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:zagrebin_v@mail.ru">Victor Zagrebin</a>
 * @version $Id: IndexTextPlainFileTest.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          The test for indexing a text/plain .txt file which contained within jcr:data property
 */

public class IndexTextPlainFileTest
   extends BaseUsecasesTest
{

   /**
    * The test for indexing a text/plain .txt file
    * 
    * @throws Exception
    */
   public void testIndexTextPlainFile() throws Exception
   {
      InputStream is = IndexTextPlainFileTest.class.getResourceAsStream("/index/test_index.txt");
      assertNotNull("Can not create an input stream from file for indexing", is);
      int size = is.available();
      byte b[] = new byte[size];
      is.read(b);
      is.close();

      Node cmr = root.addNode("cmr").addNode("categories").addNode("cmr");
      Node cool = cmr.addNode("cool", "nt:file");
      Node contentNode = cool.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue(new String(b)));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      assertNotNull("Can not create a cmr node for indexing", cmr);
      assertNotNull("Can not create a cool node for indexing", cool);
      assertNotNull("Can not create a contentNode node for indexing", contentNode);

      Node football = cmr.addNode("sports").addNode("football");
      Node news = cmr.addNode("news");
      Node uefa = football.addNode("uefa");
      Node champions_league = football.addNode("champions-league");
      Node economy = news.addNode("economy");
      Node world = news.addNode("world");
      assertNotNull("Can not create a football node for indexing", football);
      assertNotNull("Can not create a news node for indexing", news);
      assertNotNull("Can not create an uefa node for indexing", uefa);
      assertNotNull("Can not create a champions_league node for indexing", champions_league);
      assertNotNull("Can not create an economy node for indexing", economy);
      assertNotNull("Can not create a world node for indexing", world);

      session.save();

      Query q;
      String xpath = "//*[jcr:contains(., 'text file content')]";
      System.out.println("------------------ QUERY START -----------------------");
      System.out.println(xpath);
      System.out.println("------------------ QUERY END-----------------------");
      q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
      assertNotNull("Can not create a query for indexing", q);
      QueryResult result = q.execute();
      System.out.println("Information for query");

      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
         System.out.println("Node name: " + next.getName());
         System.out.println("Node type: " + next.getPrimaryNodeType().getName());
         if (next.getPrimaryNodeType().getName().equals("nt:resource"))
         {
            if (next.hasProperty("jcr:data"))
            {
               String mimeType = "";
               if (next.hasProperty("jcr:mimeType"))
               {
                  mimeType = next.getProperty("jcr:mimeType").getString();
               }
               is = next.getProperty("jcr:data").getStream();
               StandaloneContainer scontainer = StandaloneContainer.getInstance();
               DocumentReaderService service_ =
                        (DocumentReaderService) scontainer.getComponentInstanceOfType(DocumentReaderService.class);
               assertNotNull("Can not create service_ a for indexing", world);
               String found_text = service_.getContentAsText(mimeType, is);
               assertNotNull("Can not create found_text for indexing", world);
               is.close();

               System.out.println("------------------ SEARCH TEXT RESULTS START-----------------------");
               System.out.println(found_text);
               System.out.println("------------------ SEARCH TEXT RESULTS END-----------------------");
            }
         }
      }

      // fail("QUERY TEST");
   }
}
