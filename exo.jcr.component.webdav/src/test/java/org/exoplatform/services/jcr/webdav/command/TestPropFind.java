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
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.Depth;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindResponseEntity;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;

import javax.jcr.Node;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestPropFind extends BaseStandaloneTest
{

   protected Node testPropFind;

   private final static String AUTHOR = "eXoPlatform";

   private final static String WEBDAV_AUTHOR_PROPERTY = "webdav:Author";

   private final static String WEBDAV_NT_FILE = "webdav:file";

   private final static String WEBDAV_NT_RESOURCE = "exo:testResource";

   private final static String WEBDAV_TEST_PROPERTY = "webdav:test-property";

   private final static String CONTENT_TYPE = "text/xml";

   private String propFindXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\">"
         + "<D:prop xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">"
         + "<webdav:Author/><webdav:author/><webdave:DingALing/></D:prop></D:propfind>";

   private String multiPropFindXML = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\">"
      + "<D:prop xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">" + "<" + WEBDAV_TEST_PROPERTY
      + "/></D:prop></D:propfind>";

   private String propnameXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><propfind xmlns=\"DAV:\"><propname/></propfind>";

   private String allPropsXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

   public void setUp() throws Exception
   {
      super.setUp();
      testPropFind = root.addNode("TestPropFind", "nt:folder");
   }

   public void testPropfindComplexContent() throws Exception
   {

      String path = testPropFind.getPath() + "/testPropfindComplexContent";

      // prepare data
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream("file content".getBytes()), "nt:file",
            "exo:testResource", "text/plain");

      node.getNode("jcr:content").addNode("node", "nt:unstructured").setProperty("node-prop", "prop");
      node.getNode("jcr:content").setProperty("exo:prop", "prop");

      // test
      HierarchicalProperty body = new HierarchicalProperty("D:propfind", null, "DAV:");
      body.addChild(new HierarchicalProperty("D:allprop", null, "DAV:"));
      Response resp = new PropFindCommand().propfind(session, path, body, Depth.INFINITY_VALUE, "http://localhost");

      ByteArrayOutputStream bas = new ByteArrayOutputStream();
      ((PropFindResponseEntity)resp.getEntity()).write(bas);
      System.out.println(">>>>>>>>>>RESSSSSSSSSSSP>>>>>>>>>>>>>>> " + new String(bas.toByteArray()));

   }

   public void testSimplePropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      ContainerResponse containerResponseFind = service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, null);
      assertEquals(HTTPStatus.MULTISTATUS, containerResponseFind.getStatus());
   }

   /**
    * Here we test WebDAV PROPFIND method implementation for correct response 
    * if request contains encoded non-latin characters. We send a request with
    * corresponding character sequence and expect to receive response containing
    * 'href' element with URL encoded characters and 'displayname' element containing
    * non-latin characters.  
    * @throws Exception
    */
   public void testSimplePropFindWithNonLatin() throws Exception
   {
      // prepare file name and content
      String encodedfileName = "%e3%81%82%e3%81%84%e3%81%86%e3%81%88%e3%81%8a";
      String decodedfileName = URLDecoder.decode(encodedfileName, "UTF-8");
      String content = TestUtils.getFileContent();
      TestUtils.addContent(session, decodedfileName, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      TestUtils.addNodeProperty(session, decodedfileName, WEBDAV_AUTHOR_PROPERTY, AUTHOR);

      ContainerResponse response =
         service(WebDAVMethods.PROPFIND, getPathWS() + "/" + encodedfileName, "", null, allPropsXML.getBytes());

      // serialize response entity to string
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)response.getEntity();
      entity.write(outputStream);
      String resp = outputStream.toString();

      System.out.println("=======PropFind response==========");
      System.out.println(resp);
      System.out.println("=======Decoded file name==========");
      System.out.println(decodedfileName);
      System.out.println("==================================");

      assertTrue(resp.contains(encodedfileName));
      assertTrue(resp.contains(decodedfileName));
   }

   public void testPropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      TestUtils.addNodeProperty(session, file, WEBDAV_AUTHOR_PROPERTY, AUTHOR);
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, "text/xml");
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", headers, propFindXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains(WEBDAV_AUTHOR_PROPERTY));
      assertTrue(find.contains(AUTHOR));
   }

   /**
    * Here we test WebDAV PROPFIND method implementation for correct response
    * in case we are asking for a multi-valued property. It is expected 
    * to receive first value of a values list. That is basicly because WebDAV
    * actually does not support multi-valued properties in the way JCR does,
    * though it supports nested (hierarchical) properties.
    * @throws Exception
    */
   public void testNonEmptyMultiPropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      String[] propValues =
         new String[]{"No sacrifice is too great in the service of freedom.",
            "Freedom is the right of all sentient beings."};

      Node node =
         TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE,
            WEBDAV_NT_RESOURCE, CONTENT_TYPE);

      // set multi-valued property
      node.getNode("jcr:content").setProperty(WEBDAV_TEST_PROPERTY, propValues);
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);

      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", headers, multiPropFindXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue("Response should contain requested property element.", find.contains(WEBDAV_TEST_PROPERTY));
      assertTrue("Property element should contain value data.",
         find.contains("No sacrifice is too great in the service of freedom."));
   }

   /**
    * Here we test WebDAV PROPFIND method implementation for correct response
    * in case we are asking for an empty multi-valued property. It is expected 
    * to receive an empty value in response xml representation as it is 'dead'
    * property and it is not a server responsibility to support its consistency.
    * @throws Exception
    */
   public void testEmptyMultiPropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();

      Node node =
         TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE,
            WEBDAV_NT_RESOURCE, CONTENT_TYPE);
      // set empty multi-valued property
      node.getNode("jcr:content").setProperty(WEBDAV_TEST_PROPERTY, new String[]{});
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);

      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", headers, multiPropFindXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue("Response should contain requested property element.", find.contains(WEBDAV_TEST_PROPERTY));
   }

   public void testPropNames() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      TestUtils.addNodeProperty(session, file, WEBDAV_AUTHOR_PROPERTY, AUTHOR);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, propnameXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains(WEBDAV_AUTHOR_PROPERTY));
      assertTrue(find.contains("D:getlastmodified"));
   }

   public void testAllProps() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      TestUtils.addNodeProperty(session, file, WEBDAV_AUTHOR_PROPERTY, AUTHOR);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, allPropsXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains("D:getlastmodified"));
      assertTrue(find.contains(WEBDAV_AUTHOR_PROPERTY));
      assertTrue(find.contains(AUTHOR));
   }

   public void testPropWithPercent() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), WEBDAV_NT_FILE, "");
      String authorValue = "bla % bla";
      TestUtils.addNodeProperty(session, file, WEBDAV_AUTHOR_PROPERTY, authorValue);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, allPropsXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains("D:getlastmodified"));
      assertTrue(find.contains(WEBDAV_AUTHOR_PROPERTY));
      assertTrue(find.contains(authorValue));
   }
   

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
