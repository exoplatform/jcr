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

import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.proppatch.PropPatchResponseEntity;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 10 Dec 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: TestProppatch.java
 */
public class TestPropPatch extends BaseStandaloneTest
{

   private final String author = "eXoPlatform";

   private final String authorProp = "webdav:Author";

   private final String nt_webdave_file = "webdav:file";

   private final String patch =
      "<?xml version=\"1.0\"?><D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\"><D:set><D:prop><webdav:Author>"
         + author + "</webdav:Author></D:prop></D:set></D:propertyupdate>";

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   public void testPropPatchSetWithLock2() throws Exception
   {
      testPropPatchSetWithLock2(getPathWS());
   }

   public void testPropPatchSetWithLock2WithFakePathWS() throws Exception
   {
      testPropPatchSetWithLock2(getFakePathWS());
   }

   private void testPropPatchSetWithLock2(String pathWs) throws Exception
   {
      String fileName = "testPropPatchFile";
      session.getRootNode().addNode(fileName);

      Node node = session.getRootNode().addNode(fileName, nt_webdave_file);
      node.setProperty(authorProp, author);

      node.addNode("jcr:content", "nt:resource");
      Node content = node.getNode("jcr:content");
      content.setProperty("jcr:mimeType", "text/xml");
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "data");
      node.addMixin("mix:lockable");
      session.save();
      node.lock(true, true);
      session.save();

      fileName = fileName + "[2]";
      ContainerResponse response =
         serviceWithEscape(WebDAVMethods.PROPPATCH, pathWs + "/" + fileName, "", null, patch.getBytes());
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();
      assertNotNull(resp);

      Property p = node.getProperty(authorProp);
      String str = p.getString();
      assertNotNull(str);
   }

   /**
    * Here we test WebDAV PROPPATCH method implementation for correct response 
    * if request contains encoded non-latin characters. We send a request with
    * corresponding character sequence and expect to receive response containing
    * 'href' element with URL encoded characters.
    * @throws Exception
    */
   public void testPropPatchWithNonLatin() throws Exception
   {

      // prepare file names, content
      String encodedfileName = "%e3%81%82%e3%81%84%e3%81%86%e3%81%88%e3%81%8a";
      String decodedfileName = URLDecoder.decode(encodedfileName, "UTF-8");

      Node node = session.getRootNode().addNode(decodedfileName, nt_webdave_file);
      node.setProperty(authorProp, author);

      node.addNode("jcr:content", "nt:resource");
      Node content = node.getNode("jcr:content");
      content.setProperty("jcr:mimeType", "text/xml");
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "data");
      node.addMixin("mix:lockable");
      session.save();
      node.lock(true, true);
      session.save();

      ContainerResponse response =
         service(WebDAVMethods.PROPPATCH, getPathWS() + "/" + encodedfileName, "", null, patch.getBytes());

      // serialize response entity to string
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();

      assertTrue(resp.contains(encodedfileName));
      assertFalse(resp.contains(decodedfileName));
   }
}
