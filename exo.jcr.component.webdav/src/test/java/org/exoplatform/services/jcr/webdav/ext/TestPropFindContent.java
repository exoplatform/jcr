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
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.proppatch.PropPatchResponseEntity;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="work.visor.ck@gmail.com">Dmytro Katayev</a> May 20, 2009
 */
public class TestPropFindContent extends BaseStandaloneTest
{

   @Override
   protected String getRepositoryName()
   {
      // TODO Auto-generated method stub
      return null;
   }

   public void testPropFindNamedProp() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "testData");

      String outerPropName = "webdav:outerProp";
      Node outerPropNode = content.addNode(outerPropName, "exo:outerProp");
      outerPropNode.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      outerPropNode.setProperty("jcr:lastModified", Calendar.getInstance());
      outerPropNode.setProperty("jcr:data", "testData");

      String innerProp = "webdav:innerProp";
      outerPropNode.setProperty(innerProp, "innerValue");
      session.save();

      String xml =
         "" + "<D:propfind xmlns:D=\"DAV:\">" + "<D:prop xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">"
            + "<jcr:content xmlns=\"\">" + "<webdav:outerProp xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">"
            + "<webdav:innerProp/>" + "</webdav:outerProp>" + "</jcr:content>" + "</D:prop>" + "</D:propfind>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPFIND, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

   }

   public void testPropFindAllProp() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "testData");

      String outerPropName = "webdav:outerProp";
      Node outerPropNode = content.addNode(outerPropName, "exo:outerProp");
      outerPropNode.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      outerPropNode.setProperty("jcr:lastModified", Calendar.getInstance());
      outerPropNode.setProperty("jcr:data", "testData");

      String innerProp = "webdav:innerProp";
      outerPropNode.setProperty(innerProp, "innerValue");
      session.save();

      String xml =
         "" + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:propfind xmlns:D=\"DAV:\">" + "<D:allprop/>"
            + "</D:propfind>";

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DEPTH, "1");

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPFIND, getPathWS() + path, "", headers, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

   }

   public void testPropFindPropNames() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "testData");

      String outerPropName = "webdav:outerProp";
      Node outerPropNode = content.addNode(outerPropName, "exo:outerProp");
      outerPropNode.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      outerPropNode.setProperty("jcr:lastModified", Calendar.getInstance());
      outerPropNode.setProperty("jcr:data", "testData");

      String innerProp = "webdav:innerProp";
      outerPropNode.setProperty(innerProp, "innerValue");
      session.save();

      String xml =
         "" + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:propfind xmlns:D=\"DAV:\">" + "<D:allprop/>"
            + "</D:propfind>";

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DEPTH, "1");

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPFIND, getPathWS() + path, "", headers, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

   }

   public static Node getContentNode(Node node) throws RepositoryException
   {
      return node.getNode("jcr:content");
   }

}
