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
package org.exoplatform.services.jcr.webdav.ext;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestCaching extends BaseStandaloneTest
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

   public void testNotModifiedSince() throws Exception
   {
      Node contentNode = node.getNode("jcr:content");
      Property lastModifiedProperty = contentNode.getProperty("jcr:lastModified");
      
      SimpleDateFormat dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.IF_MODIFIED_SINCE_PATTERN);
      Calendar lastModifiedDate = lastModifiedProperty.getDate();
      
      lastModifiedDate.roll(Calendar.WEEK_OF_MONTH, -1);
      // Rollback If-Modified-Since date a week earlier.
      String ifModifiedDate = dateFormat.format(lastModifiedDate.getTime());
      
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.IF_MODIFIED_SINCE, ifModifiedDate);
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + path, "", headers, null);
      
      assertEquals(HTTPStatus.OK, response.getStatus());
   }
   
   public void testIfModifiedSince() throws Exception
   {
      Node contentNode = node.getNode("jcr:content");
      Property lastModifiedProperty = contentNode.getProperty("jcr:lastModified");
      
      SimpleDateFormat dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.IF_MODIFIED_SINCE_PATTERN);
      Calendar lastModifiedDate = lastModifiedProperty.getDate();
      
      lastModifiedDate.add(Calendar.WEEK_OF_MONTH, 1);
      String ifModifiedDate = dateFormat.format(lastModifiedDate.getTime());
      
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.IF_MODIFIED_SINCE, ifModifiedDate);
      ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + path, "", headers, null);
      
      assertEquals(HTTPStatus.NOT_MODIFIED, response.getStatus());
   }
   
   public void testCacheConf() throws Exception
   {
      ArrayList<CacheControlType> testValues = new ArrayList<CacheControlType>();
      testValues.add(new CacheControlType("text/xml", "max-age=1800"));
      testValues.add(new CacheControlType("text/pdf", "max-age=777"));
      testValues.add(new CacheControlType("image/jpg", "max-age=3600"));
      testValues.add(new CacheControlType("image/gif", "max-age=555"));
      testValues.add(new CacheControlType("test/test", "no-cache"));
      testValues.add(new CacheControlType("*/*", "no-cache"));

      Node contentNode = node.getNode("jcr:content");

      for (CacheControlType cacheControlType : testValues)
      {
         contentNode.setProperty("jcr:mimeType", cacheControlType.getContentType());
         contentNode.getSession().save();
         ContainerResponse response = service(WebDAVMethods.GET, getPathWS() + path, "", null, null);
         String cacheControlHeader = response.getHttpHeaders().get(HttpHeaders.CACHE_CONTROL).toString();
         cacheControlHeader = cacheControlHeader.substring(1, cacheControlHeader.length() - 1);
         assertEquals(cacheControlHeader, cacheControlType.getCacheValue());
      }
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
