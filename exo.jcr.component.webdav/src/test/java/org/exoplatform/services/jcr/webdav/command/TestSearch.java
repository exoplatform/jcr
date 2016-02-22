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
package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.dasl.SearchResultResponseEntity;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;


/**
 * Created by The eXo Platform SAS. <br>
 * Date: 15 Dec 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: testSearch.java
 */
public class TestSearch extends BaseStandaloneTest
{

   private String fileName = TestUtils.getFileName();

   private final String fileContent = "TEST FILE CONTENT...";

   private final String basicSql = "<D:searchrequest xmlns:D='DAV:'>" + "<D:sql>"
      + "SELECT * FROM  nt:resource WHERE contains(*, 'TEST')" + "</D:sql>" + "</D:searchrequest>";

   private final String pathSql = "<D:searchrequest xmlns:D='DAV:'>" + "<D:sql>"
            + "SELECT * FROM nt:base WHERE jcr:path LIKE '/node[%]/%'" + "</D:sql>" + "</D:searchrequest>";

   public void testBasicSearch() throws Exception
   {
      testBasicSearch(getPathWS());
   }

   public void testBasicSearchWithFakePathWS() throws Exception
   {
      testBasicSearch(getFakePathWS());
   }

   // String body =
   // "<D:searchrequest xmlns:D='DAV:'>" +
   // "<D:xpath>" +
   // "element(*, nt:resource)[jcr:contains(jcr:data, '*F*')]" +
   // "</D:xpath>" +
   // "</D:searchrequest>";
   private void testBasicSearch(String pathWs) throws Exception
   {
      session.getRootNode().addNode(TextUtil.relativizePath(fileName));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, fileName, inputStream, defaultFileNodeType, MediaType.TEXT_PLAIN);
      ContainerResponse response = service(WebDAVMethods.SEARCH, pathWs, "", null, basicSql.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      SearchResultResponseEntity entity = (SearchResultResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String result = outputStream.toString();
      assertTrue(result.contains(fileName));
   }

   public void testPathSearch() throws Exception
   {
      Node testRoot = session.getRootNode().addNode("node", defaultFolderNodeType);
      Node node = testRoot.addNode("addedNode", defaultFileNodeType);
      node.addNode("jcr:content", "nt:resource");
      Node content = node.getNode("jcr:content");
      content.setProperty("jcr:mimeType", MediaType.TEXT_PLAIN);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", new ByteArrayInputStream("Text".getBytes()));
      session.save();
      
      ContainerResponse response = service(WebDAVMethods.SEARCH, getPathWS(), "", null, pathSql.getBytes());
      SearchResultResponseEntity entity = (SearchResultResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      outputStream.toByteArray();
      List<String> found = parseNodeNames(outputStream.toByteArray());
      assertEquals(1, found.size());
   }

   /**
    * Here we test WebDAV SEARCH method implementation for correct response 
    * if request contains encoded non-latin characters. We send a request with
    * corresponding character sequence and expect to receive response containing
    * 'href' element with URL encoded characters and 'displayname' element containing
    * non-latin characters.    
    * @throws Exception
    */
   public void testBasicSearchWithNonLatin() throws Exception
   {
      // prepare file name, content
      String encodedfileName = "%e3%81%82%e3%81%84%e3%81%86%e3%81%88%e3%81%8a";
      String decodedfileName = URLDecoder.decode(encodedfileName, "UTF-8");
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, decodedfileName, inputStream, defaultFileNodeType, MediaType.TEXT_PLAIN);

      ContainerResponse response = service(WebDAVMethods.SEARCH, getPathWS(), "", null, basicSql.getBytes());

      // serialize response entity to string
      SearchResultResponseEntity entity = (SearchResultResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString("UTF-8");

      assertTrue(resp.contains(encodedfileName));
      assertTrue(resp.contains(decodedfileName));

   }

   @Override
   protected String getRepositoryName()
   {
      return null;
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
      QName name = QName.valueOf("{DAV:}href");
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
}
