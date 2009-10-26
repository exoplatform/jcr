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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestGet extends BaseStandaloneTest
{

   private String path = TestUtils.getFileName();

   private String fileContent = TestUtils.getFileContent();
   
   private Node node;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      node = TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
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
         sw.write(buffer, 0, n);
      String str = sw.toString();
      assertEquals(fileContent, str);
   }
   
   public void testIfModifiedSince() throws Exception{
      Node contentNode = node.getNode("jcr:content");
      Property lastModifiedProperty = contentNode.getProperty("jcr:lastModified");
      String formatPattern = "EEE, dd MMM yyyy HH:mm:ss z";
      SimpleDateFormat dateFormat = new SimpleDateFormat(formatPattern, Locale.ENGLISH);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      String lastModified = dateFormat.format(lastModifiedProperty.getDate().getTime());
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.IF_MODIFIED_SINCE, lastModified);
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + path, "", headers, null);
      assertEquals(HTTPStatus.NOT_MODIFIED, response.getStatus());
   }

   public void testNotFoundGet() throws Exception
   {
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + "/not-found" + path, "", null, null);
      assertEquals(HTTPStatus.NOT_FOUND, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
