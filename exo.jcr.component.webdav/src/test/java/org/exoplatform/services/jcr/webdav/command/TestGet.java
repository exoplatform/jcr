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
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.ext.provider.XSLTStreamingOutput;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestGet extends BaseStandaloneTest
{

   private String path = TestUtils.getFileName();

   private String fileContent = TestUtils.getFileContent();

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
   }

   public void testSimpleGet() throws Exception
   {
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());
      FileInputStream content = (FileInputStream)response.getEntity();
      Reader r = new InputStreamReader(content);
      StringWriter sw = new StringWriter();
      char[] buffer = new char[1024];
      for (int n; (n = r.read(buffer)) != -1;)
      {
         sw.write(buffer, 0, n);
      }
      String str = sw.toString();
      assertEquals(fileContent, str);
   }

   public void testNotFoundGet() throws Exception
   {
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + "/not-found" + path, "", null, null);
      assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
   }

   /**
    * Details can be found here: https://jira.jboss.org/browse/EXOJCR-956
    * @throws Exception
    */
   public void testMissingJcrLastModifiedProperty() throws Exception
   {
      File file = new File("src/test/resources/rh_nodetype.xml");
      assertTrue("src/test/resources/rh_nodetype.xml not found", file.exists());
      FileInputStream fis = new FileInputStream(file);

      session.getWorkspace().getNamespaceRegistry().registerNamespace("rh", "www.vn.vnn");
      session.getWorkspace().getNodeTypesHolder().registerNodeTypes(fis, ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
         NodeTypeDataManager.TEXT_XML);

      Node podcast = session.getRootNode().addNode("podcast", "rh:podcast");

      Node nodeToAdd = podcast.addNode("rh:podcastFile", "nt:file");
      Node contentNodeOfNodeToAdd = nodeToAdd.addNode("jcr:content", "nt:resource");
      contentNodeOfNodeToAdd.setProperty("jcr:data", new FileInputStream("src/test/resources/test.txt"));
      contentNodeOfNodeToAdd.setProperty("jcr:mimeType", "text/plain");
      contentNodeOfNodeToAdd.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();

      podcast.addMixin("mix:versionable");
      session.save();

      VersionImpl v = (VersionImpl)podcast.checkin();
      session.save();

      podcast.checkout();
      session.save();

      String path =
         getPathWS() + "/jcr:system/jcr:versionStorage/" + v.getContainingHistory().getIdentifier()
            + "/1/jcr:frozenNode/rh:podcastFile";

      ContainerResponse response = service(WebDAVMethods.GET, path, "", null, null);
      assertEquals("Successful result expected (200), but actual is: " + response.getStatus(), 200, response
         .getStatus());

   }

   /**
    * Tests if date passed through header If-modified-since of GET method parsed correctly.
    * Details can be found here: http://jira.exoplatform.org/browse/JCR-1470
    * @throws Exception
    */
   public void testIfModifiedSinceDateParsing() throws Exception
   {
      Node fileNode = session.getRootNode().addNode("node", "nt:file");
      fileNode.addMixin("mix:versionable");

      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:mimeType", "text/plain");

      Calendar creationDate = Calendar.getInstance();
      Calendar IfModifiedSince = Calendar.getInstance();
      IfModifiedSince.add(Calendar.HOUR, -2);
      creationDate.add(Calendar.HOUR, -1);
      contentNode.setProperty("jcr:data", creationDate);
      contentNode.setProperty("jcr:lastModified", creationDate);

      session.save();

      fileNode.checkin();
      fileNode.checkout();

      String path =
         getPathWS() + "/" + fileNode.getName() + "?time=" + IfModifiedSince.getTimeInMillis() + "&version=1";

      SimpleDateFormat dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.IF_MODIFIED_SINCE_PATTERN, Locale.US);
      String ifModifiedSinceDate = dateFormat.format(IfModifiedSince.getTime());

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.IF_MODIFIED_SINCE, ifModifiedSinceDate);
      ContainerResponse response = service(WebDAVMethods.GET, path, "", headers, null);
      assertEquals(HTTPStatus.OK, response.getStatus());

      headers.clear();

      IfModifiedSince.add(Calendar.HOUR, +4);
      ifModifiedSinceDate = dateFormat.format(IfModifiedSince.getTime());
      headers.add(ExtHttpHeaders.IF_MODIFIED_SINCE, ifModifiedSinceDate);
      response = service(WebDAVMethods.GET, path, "", headers, null);
      assertEquals(HTTPStatus.NOT_MODIFIED, response.getStatus());
   }

   public void testXSLTParamsPassing() throws Exception
   {
      String strToTest = "/absolute/path/to/file";
      String folderName = TestUtils.getFolderName();
      TestUtils.addFolder(session, folderName, defaultFolderNodeType, "");
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + folderName, "", null, null);

      assertEquals(HTTPStatus.OK, response.getStatus());

      XSLTStreamingOutput XSLTout = (XSLTStreamingOutput)response.getEntity();
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      XSLTout.write(byteOut);

      assertTrue(byteOut.toString().contains(strToTest));

   }

   /**
    * We test for parent href to be contained in response.
    * We GET a collection and receive an html. Html should contain
    * a href to parent collection of requested collection. 
    * @throws Exception
    */
   public void testParentHrefForGetColRequest() throws Exception
   {
      String folderOne = TestUtils.getFolderName();
      String folderTwo = folderOne + TestUtils.getFolderName();

      // add collections
      TestUtils.addFolder(session, folderOne, defaultFolderNodeType, "");
      TestUtils.addFolder(session, folderTwo, defaultFolderNodeType, "");

      // get a sub-collection
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + folderTwo, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());

      // serialize response entity to string
      XSLTStreamingOutput XSLTout = (XSLTStreamingOutput)response.getEntity();
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      XSLTout.write(byteOut);

      assertTrue("Response should contain parent collection href", byteOut.toString().contains(folderOne));
   }

   /**
    * We test for parent href to be contained in response. We GET a collection,
    * which has non latin letters in its name, and receive an html like response. 
    * Response should contain href to parent collection of requested collection.
    * Details can be found here: https://issues.jboss.org/browse/EXOJCR-1379
    * @throws Exception
    */
   public void testParentHrefForGetColWithNonLatinNameRequest() throws Exception
   {
      // "%40" corresponds to '@' symbol
      String folderOne = TestUtils.getFolderName() + "%40";
      String folderTwo = folderOne + TestUtils.getFolderName() + "%40";

      ContainerResponse response;

      //add collections
      TestUtils.addFolder(session, URLDecoder.decode(folderOne, "UTF-8"), defaultFolderNodeType, "");
      TestUtils.addFolder(session, URLDecoder.decode(folderTwo, "UTF-8"), defaultFolderNodeType, "");

      //get a sub-collection
      response = service(WebDAVMethods.GET, getPathWS() + folderTwo, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());

      // serialize response entity to string
      XSLTStreamingOutput XSLTout = (XSLTStreamingOutput)response.getEntity();
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      XSLTout.write(byteOut);

      assertTrue("Response should contain parent collection href", byteOut.toString().contains(folderOne));
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
