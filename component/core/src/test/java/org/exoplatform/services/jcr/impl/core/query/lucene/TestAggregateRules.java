/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query.lucene;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.exoplatform.services.jcr.impl.core.query.BaseQueryTest;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestAggregateRules.java 11931 2008-03-14 09:29:17Z serg $
 */

public class TestAggregateRules
   extends BaseQueryTest
{

   public final String fileName = "FileName";

   public void testAdditionFile() throws Exception
   {
      try
      {
         // add aggregate rules to indexing configuration

         String conf =
                  "<?xml version=\"1.0\"?>"
                           + "\n"
                           + "<!DOCTYPE configuration SYSTEM \"http://www.exoplatform.org/dtd/indexing-configuration-1.0.dtd\">"
                           + "\n" + "<configuration xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"" + "\n"
                           + " xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\">" + "\n"
                           + "<aggregate primaryType=\"nt:file\">" + "\n" + "<include>jcr:content</include>" + "\n"
                           + "</aggregate>" + "\n" + "</configuration>";

         InputStream is = new StringBufferInputStream(conf);

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         builder.setEntityResolver(new IndexingConfigurationEntityResolver());
         Element indexingConfiguration = builder.parse(is).getDocumentElement();
         NodeList lst = indexingConfiguration.getChildNodes();

         org.w3c.dom.Node configNode = lst.item(0).getNextSibling();

         assertNotNull(configNode);

         IndexingConfigurationImpl indexingConfigurationImpl =
                  (IndexingConfigurationImpl) defaultSearchIndex.getIndexingConfig();
         indexingConfigurationImpl.addAggregateRule(configNode);

         // create node nt:file
         Node node = session.getRootNode().addNode(fileName, "nt:file");
         Node content = node.addNode("jcr:content", "nt:resource");
         content.setProperty("jcr:mimeType", "text/plain");
         content.setProperty("jcr:lastModified", Calendar.getInstance());
         content.setProperty("jcr:data", new ByteArrayInputStream("The quick brown fox jump over the lazy dog"
                  .getBytes()));
         session.save();

         QueryManager qman = this.workspace.getQueryManager();

         Query q = qman.createQuery("SELECT * FROM nt:file " + " WHERE  CONTAINS(., 'fox')", Query.SQL);

         QueryResult res = q.execute();

         assertEquals(1, res.getNodes().getSize());

         q = qman.createQuery("SELECT * FROM nt:resource " + " WHERE  CONTAINS(., 'fox')", Query.SQL);

         res = q.execute();
         assertEquals(1, res.getNodes().getSize());

         // tear down
         indexingConfigurationImpl.removeLastAggregateRule();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }
}
