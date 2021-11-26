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

package org.exoplatform.services.jcr.usecases.index;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.document.DocumentReaderService;
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
 * @version $Id: IndexExcelFileTest.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          The test for indexing an excel .xls file which contained within jcr:data property
 */

public class IndexExcelFileTest extends BaseUsecasesTest
{

   /**
    * The test for indexing an excel .xls file
    * 
    * @throws Exception
    */
   public void testIndexTextPlainFile() throws Exception
   {
      InputStream is = IndexExcelFileTest.class.getResourceAsStream("/index/test_index.xls");
      assertNotNull("Can not create an input stream from file for indexing", is);

      Node cmr = root.addNode("cmr").addNode("categories").addNode("cmr");
      Node cool = cmr.addNode("cool", "nt:file");
      Node contentNode = cool.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/excel");
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
      String xpath = "//*[jcr:contains(., 'excel file content')]";
      q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
      assertNotNull("Can not create a query for indexing", q);
      QueryResult result = q.execute();

      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         Node next = it.nextNode();
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
                  (DocumentReaderService)scontainer.getComponentInstanceOfType(DocumentReaderService.class);
               assertNotNull("Can not create service_ a for indexing", world);
               String found_text = service_.getContentAsText(mimeType, is);
               assertNotNull("Can not create found_text for indexing", world);
               is.close();
            }
         }
      }
      // fail("QUERY TEST");
   }
}
