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
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;

import javax.ws.rs.core.MediaType;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 15 Dec 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: testSearch.java
 */
public class TestSearch extends BaseStandaloneTest
{

   private String fileName = TestUtils.getFileName();

   private final String fileContent = "TEST FILE CONTENT...";

   private final String sql = "<D:searchrequest xmlns:D='DAV:'>" + "<D:sql>"
      + "SELECT * FROM  nt:resource WHERE contains(*, 'TEST')" + "</D:sql>" + "</D:searchrequest>";


   public void testBasicSearch() throws Exception
   {

      // String body =
      // "<D:searchrequest xmlns:D='DAV:'>" +
      // "<D:xpath>" +
      // "element(*, nt:resource)[jcr:contains(jcr:data, '*F*')]" +
      // "</D:xpath>" +
      // "</D:searchrequest>";


      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, fileName, inputStream, defaultFileNodeType, MediaType.TEXT_PLAIN);
      ContainerResponse response = service(WebDAVMethods.SEARCH, getPathWS(), "", null, sql.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      SearchResultResponseEntity entity = (SearchResultResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String result = outputStream.toString();
      assertTrue(result.contains(fileName));
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

      ContainerResponse response = service(WebDAVMethods.SEARCH, getPathWS(), "", null, sql.getBytes());

      // serialize response entity to string
      SearchResultResponseEntity entity = (SearchResultResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();

      System.out.println("=======Search response============");
      System.out.println(resp);
      System.out.println("=======Decoded file name==========");
      System.out.println(decodedfileName);
      System.out.println("==================================");

      assertTrue(resp.contains(encodedfileName));
      assertTrue(resp.contains(decodedfileName));
      
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }
}
