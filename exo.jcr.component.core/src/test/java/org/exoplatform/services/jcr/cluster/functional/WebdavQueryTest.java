/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.cluster.functional;

import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.common.http.client.ModuleException;
import org.exoplatform.services.jcr.cluster.BaseClusteringFunctionalTest;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.query.Query;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;

/**
 * Class contains set of query tests for cluster environment
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: WebdavQueryTest.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class WebdavQueryTest extends BaseClusteringFunctionalTest
{
   public static final String MIME_TEXT_PLAIN = "text/plain";

   public static final String MIME_TEXT_PATCH = "text/x-patch";

   public static final String MIME_TEXT_HTML = "text/html";

   /**
    * Delay between adding nodes and querying repository (in ms) 
    */
   public static final int SLEEP_BEFORE_QUERY = 6000;

   /**
    * Full-text query tests
    */
   public void testFullTextSearch() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      // Nodes with some text, with unique words in each one in form of <name><content>
      Map<String, String> nodes = new HashMap<String, String>();
      nodes
         .put(
            "JCR_Overview",
            "A JCR is a type of Object Database tailored to the storage, searching, and retrieval of hierarchical data. The JCR API grew o"
               + "ut of the needs of content management systems, which require storage of documents and other binary objects with associated me"
               + "tadata; however, the API is applicable to many additional types of application. In addition to object storage, the JCR provid"
               + "es: APIs for versioning of data; transactions; observation of changes in data; and import or export of data to XML in a standard way.");
      nodes
         .put(
            "JCR_Structure",
            "The data in a JCR consists of a tree of Nodes with associated Properties. Data is stored in the Properties, which may hold simple "
               + "values such as numbers and strings or binary data of arbitrary length. Nodes may optionally have one or more types associated with"
               + " them which dictate the kinds of properties, number and type of child nodes, and certain behavioral characteristics of the nodes. API");

      nodes
         .put(
            "JCR_Queries",
            "A JCR can be queried with XPathQuery, can export portions of its tree to XML in two standard formats and can import hierarchies directly"
               + " from XML. A JCR may optionally support a standardized form of SQL for queries. The Apache Jackrabbit reference implementation of "
               + "JCR also supports the integration of the Apache Lucene search engine to give full text searches of data in the repository.");

      nodes.put("JCR_Impl",
         "eXo Platform JCR implementation on the company wiki. eXo Platform 2 article on theserverside");
      // add nodes
      for (Entry<String, String> entry : nodes.entrySet())
      {
         conn.addNode(entry.getKey(), entry.getValue().getBytes(), MIME_TEXT_PLAIN);
      }
      // wait for indexer to flush volatile index
      sleep();

      // map containing test-case: <SQL query> : <expected nodes>
      Map<String, String[]> sqlCases = new HashMap<String, String[]>();
      sqlCases.put("SELECT * FROM nt:base WHERE CONTAINS(*,'tailored')", new String[]{"JCR_Overview"});
      sqlCases.put("SELECT * FROM nt:base WHERE CONTAINS(*,'XPathQuery')", new String[]{"JCR_Queries"});
      sqlCases.put("SELECT * FROM nt:resource WHERE CONTAINS(*,'API')", new String[]{"JCR_Structure", "JCR_Overview"});
      assertQuery(sqlCases, Query.SQL);

      // map containing test-case: <XPATH query> : <expected nodes>
      Map<String, String[]> xpathCases = new HashMap<String, String[]>();
      xpathCases.put("//element(*, nt:base)[jcr:contains(.,'tailored')]", new String[]{"JCR_Overview"});
      xpathCases.put("//element(*, nt:base)[jcr:contains(.,'XPathQuery')]", new String[]{"JCR_Queries"});
      xpathCases.put("//element(*, nt:resource)[jcr:contains(.,'API')]", new String[]{"JCR_Structure", "JCR_Overview"});
      assertQuery(xpathCases, Query.XPATH);
      // remove created nodes
      for (Entry<String, String> entry : nodes.entrySet())
      {
         conn.removeNode(entry.getKey());
      }
   }

   /**
    * Simple test, searching nodes by given path and concrete name.
    */
   public void testPathSearch() throws Exception
   {
      String testLocalRootName = "testPathSearch";
      JCRWebdavConnection conn = getConnection();
      conn.addDir(testLocalRootName);
      List<String> expected = new ArrayList<String>();
      expected.add("exoString");
      expected.add("exoBoolean");
      expected.add("exoInteger");
      expected.add("exoLong");
      expected.add("exoFloat");
      expected.add("exoDouble");

      for (String name : expected)
      {
         conn.addNode(testLocalRootName + "/" + name, "_data_".getBytes());
      }
      // wait for indexer to flush volatile index
      sleep();

      // map containing test-case: <SQL query> : <expected nodes>
      Map<String, String[]> sqlCases = new HashMap<String, String[]>();
      sqlCases.put("SELECT * FROM nt:base WHERE jcr:path LIKE '/" + testLocalRootName
         + "[%]/%' AND NOT jcr:path LIKE '/" + testLocalRootName + "[%]/%/%' ", expected.toArray(new String[expected
         .size()]));
      sqlCases.put("SELECT * FROM nt:base WHERE fn:name() = 'exoString'", new String[]{"exoString"});
      assertQuery(sqlCases, Query.SQL);

      // map containing test-case: <XPATH query> : <expected nodes>
      Map<String, String[]> xpathCases = new HashMap<String, String[]>();
      xpathCases.put("/jcr:root/" + testLocalRootName + "/ element(*, nt:base)", expected.toArray(new String[expected
         .size()]));
      xpathCases.put("//element(*,nt:file)[fn:name() = 'exoString']", new String[]{"exoString"});
      assertQuery(xpathCases, Query.XPATH);

      conn.removeNode(testLocalRootName);
   }

   /**
    * Test, searching over the repository nodes with concrete value of concrete property.
    * jcr:mimeType is used for querying purposes. 
    */
   public void testPropertyValueSearch() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      // Nodes with concrete mimetype in form of <name><content>
      Map<String, String> nodes = new HashMap<String, String>();
      // text/plain
      nodes.put("TextDescription", MIME_TEXT_PLAIN);
      nodes.put("SmallNote", MIME_TEXT_PLAIN);
      nodes.put("CalendarMemo", MIME_TEXT_PLAIN);
      nodes.put("GetThisDone", MIME_TEXT_PLAIN);
      // text/patch
      nodes.put("CriticalPath", MIME_TEXT_PATCH);
      nodes.put("BrokenPatch", MIME_TEXT_PATCH);
      // text/html
      nodes.put("FirstPage", MIME_TEXT_HTML);
      nodes.put("AboutGateIn", MIME_TEXT_HTML);
      nodes.put("LicenseAgreement", MIME_TEXT_HTML);
      nodes.put("HomePage", MIME_TEXT_HTML);
      nodes.put("StrangePage", MIME_TEXT_HTML);

      // add nodes
      for (Entry<String, String> entry : nodes.entrySet())
      {
         conn.addNode(entry.getKey(), "content".getBytes(), entry.getValue());
      }
      // wait for indexer to flush volatile index
      sleep();

      // map containing test-case: <SQL query> : <expected nodes>
      Map<String, String[]> sqlCases = new HashMap<String, String[]>();
      sqlCases.put("SELECT * FROM nt:resource WHERE jcr:mimeType ='" + MIME_TEXT_PLAIN + "'", getNodesByMime(nodes,
         MIME_TEXT_PLAIN));
      sqlCases.put("SELECT * FROM nt:resource WHERE jcr:mimeType ='" + MIME_TEXT_HTML + "'", getNodesByMime(nodes,
         MIME_TEXT_HTML));
      sqlCases.put("SELECT * FROM nt:resource WHERE jcr:mimeType LIKE 'text%'", nodes.keySet().toArray(
         new String[nodes.size()]));
      assertQuery(sqlCases, Query.SQL);

      // map containing test-case: <XPATH query> : <expected nodes>
      Map<String, String[]> xpathCases = new HashMap<String, String[]>();
      xpathCases.put("//element(*,nt:resource)[@jcr:mimeType='" + MIME_TEXT_PLAIN + "']", getNodesByMime(nodes,
         MIME_TEXT_PLAIN));
      xpathCases.put("//element(*,nt:resource)[@jcr:mimeType='" + MIME_TEXT_HTML + "']", getNodesByMime(nodes,
         MIME_TEXT_HTML));
      xpathCases.put("//element(*,nt:resource)[jcr:like(@jcr:mimeType, 'text%')]", nodes.keySet().toArray(
         new String[nodes.size()]));
      assertQuery(xpathCases, Query.XPATH);

      // remove created nodes
      for (Entry<String, String> entry : nodes.entrySet())
      {
         conn.removeNode(entry.getKey());
      }
   }

   /**
    * Performs sequence of queries and asserts received results
    * 
    * @param conn
    * @param queryCases
    *          map containing test-case: <query> : <expected nodes>
    * @param lang
    *          Query.SQL or Query.XPATH
    * @throws IOException
    * @throws ModuleException
    * @throws XMLStreamException
    * @throws FactoryConfigurationError
    */
   private void assertQuery(Map<String, String[]> queryCases, String lang) throws IOException, ModuleException,
      XMLStreamException, FactoryConfigurationError
   {
      if (lang.equals(Query.SQL) || lang.equals(Query.XPATH))
      {
         for (JCRWebdavConnection connection : getConnections())
         {
            for (Entry<String, String[]> entry : queryCases.entrySet())
            {
               HTTPResponse response =
                  lang.equals(Query.SQL) ? connection.sqlQuery(entry.getKey()) : connection.xpathQuery(entry.getKey());
               assertEquals(207, response.getStatusCode());
               List<String> found;
               assertEquals(207, response.getStatusCode());
               found = parseNodeNames(response.getData());
               assertTrue("Lists are not equals:\n*found:\t" + found + "\n*expected:\t" + entry.getValue(),
                  compareLists(Arrays.asList(entry.getValue()), found));
            }
         }
      }
      else
      {
         fail("Unsupported query language:" + lang);
      }
   }

   /**
    * Given map nodesMap should contain entry: <nodeName>:<mime-type>, this method returns array with names of 
    * nodes that are only of given mime-type.
    * 
    * @param nodesMap
    * @param mime
    * @return
    */
   private String[] getNodesByMime(Map<String, String> nodesMap, String mime)
   {
      List<String> filteredNodes = new ArrayList<String>();
      for (Entry<String, String> entry : nodesMap.entrySet())
      {
         if (entry.getValue().equals(mime))
         {
            filteredNodes.add(entry.getKey());
         }
      }
      return filteredNodes.toArray(new String[filteredNodes.size()]);
   }

   /**
    * returns true if lists are equals (order doesn't matter)
    * 
    * @param expected
    * @param found
    * @return
    */
   private boolean compareLists(Collection<String> expected, Collection<String> found)
   {
      if (expected == null || found == null)
      {
         return false;
      }
      return expected.containsAll(found) && found.containsAll(expected);
   }

   /**
    * Extracts names of nodes from response XML
    * 
    * @param data
    * @return
    * @throws XMLStreamException
    * @throws FactoryConfigurationError
    * @throws IOException
    */
   private List<String> parseNodeNames(byte[] data) throws XMLStreamException, FactoryConfigurationError, IOException
   {
      // flag, that notifies when parser is inside <D:displayname></D:displayname> 
      boolean displayName = false;
      //Set<String> nodes = new HashSet<String>();
      List<String> nodes = new ArrayList<String>();
      InputStream input = new ByteArrayInputStream(data);
      XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
      QName name = QName.valueOf("{DAV:}displayname");
      try
      {
         while (reader.hasNext())
         {
            int eventCode = reader.next();
            switch (eventCode)
            {
               case StartElement.START_ELEMENT : {
                  // if {DAV:}displayname opening element 
                  if (reader.getName().equals(name))
                  {
                     displayName = true;
                  }
                  break;
               }
               case StartElement.CHARACTERS : {
                  if (displayName)
                  {
                     // currently reader is inside <D:displayname>nodeName</D:displayname>
                     // adding name to list if not empty
                     String nodeName = reader.getText();
                     if (nodeName != null && !nodeName.equals(""))
                     {
                        nodes.add(nodeName);
                     }
                  }
                  break;
               }
               default : {
                  displayName = false;
                  break;
               }
            }
         }
      }
      finally
      {
         reader.close();
         input.close();
      }
      return new ArrayList<String>(nodes);
   }

   /**
    * Sleep for SLEEP_BEFORE_QUERY seconds. This is needed because Indexer is asynchronous and 
    * volatile index can be flushed after some time. 
    */
   private void sleep()
   {
      try
      {
         Thread.sleep(SLEEP_BEFORE_QUERY);
      }
      catch (InterruptedException e)
      {
      }
   }
}
