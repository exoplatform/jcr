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
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="work.visor.ck@gmail.com">Dmytro Katayev</a> May 7, 2009
 */
public class TestPropPatchContent extends BaseStandaloneTest
{

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   public void testProppatchSetContentProp() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "testData");

      String propName1 = "webdav:prop1";
      String propName2 = "webdav:prop2";

      content.setProperty(propName1, "value1");
      content.setProperty(propName2, "value2");
      session.save();

      String xml =
         ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
            + "<D:set>" + "<D:prop>" + "<webdav:Author>Author</webdav:Author>" + "<jcr:content>"
            + "<webdav:prop1>testValue1</webdav:prop1>" + "<webdav:prop2>testValue2</webdav:prop2>" + "</jcr:content>"
            + "</D:prop>" + "</D:set>" + "</D:propertyupdate>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

      assertEquals("testValue1", content.getProperty(propName1).getValue().getString());
      assertEquals("testValue2", content.getProperty(propName2).getValue().getString());

   }

   public void testProppatchRemoveContentProp() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "testData");

      String propName1 = "webdav:prop1";

      content.setProperty(propName1, "value1");
      session.save();

      String xml =
         ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
            + "<D:remove>" + "<D:prop>" + "<webdav:Author>Author</webdav:Author>" + "<jcr:content>" + "<webdav:prop1/>"
            + "</jcr:content>" + "</D:prop>" + "</D:remove>" + "</D:propertyupdate>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

      try
      {
         content.getProperty(propName1);
         fail();
      }
      catch (PathNotFoundException e)
      {
         // Success there is no such property.
      }

   }

   public void testProppatchSetJcrData() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "data");

      String propName1 = "jcr:data";

      content.setProperty(propName1, "data");
      session.save();

      String xml =
         ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
            + "<D:set>" + "<D:prop>" + "<jcr:content>" + "<jcr:data>testData</jcr:data>" + "</jcr:content>"
            + "</D:prop>" + "</D:set>" + "</D:propertyupdate>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

      Property p = content.getProperty(propName1);
      System.out.println(p.getString());
      assertEquals("data", p.getString());

   }

   public void testProppatchRemoveJcrData() throws Exception
   {

      Node node = session.getRootNode().addNode("propPatchContentNode", "nt:file");

      Node content = node.addNode("jcr:content", "exo:testContentResource");

      content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", "data");

      String propName1 = "jcr:data";

      content.setProperty(propName1, "data");
      session.save();

      String xml =
         ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
            + "<D:set>" + "<D:prop>" + "<jcr:content>" + "<jcr:data/>" + "</jcr:content>" + "</D:prop>" + "</D:set>"
            + "</D:propertyupdate>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);

      content = getContentNode(node);

      Property p = content.getProperty(propName1);
      assertEquals("data", p.getString());

   }

   public void testProppatchSetHierContentProp() throws Exception
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
         ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
            + "<D:set>" + "<D:prop>" + "<jcr:content>" + "<webdav:outerProp>" + "<jcr:data>innerTestValue</jcr:data>"
            + "</webdav:outerProp>" + "</jcr:content>" + "</D:prop>" + "</D:set>" + "</D:propertyupdate>";

      String path = node.getPath();
      ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() + path, "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      entity.write(outputStream);
      // ByteArrayOutputStream bas = new ByteArrayOutputStream();
      // ((PropPatchResponseEntity)entity).write(bas);
      // System.out.println("\t"+new String(bas.toByteArray()));

      content = getContentNode(node);
      Property innerProperty = content.getNode(outerPropName).getProperty(innerProp);
      assertEquals("innerTestValue", innerProperty.getValue().getString());

   }

   // public void testProppatchRemoveHierContentProp() throws Exception {
   //
   // Node node = session.getRootNode().addNode("propPatchContentNode",
   // "nt:file");
   //  
   // Node content = node.addNode("jcr:content", "exo:testContentResource");
   //  
   // content.setProperty("jcr:mimeType", MediaType.TEXT_XML);
   // content.setProperty("jcr:lastModified", Calendar.getInstance());
   // content.setProperty("jcr:data", "testData");
   //  
   // String outerPropName = "webdav:outerProp";
   // Node outerPropNode = content.addNode(outerPropName, "exo:outerProp");
   // outerPropNode.setProperty("jcr:mimeType", MediaType.TEXT_XML);
   // outerPropNode.setProperty("jcr:lastModified", Calendar.getInstance());
   // outerPropNode.setProperty("jcr:data", "testData");
   //  
   // String innerProp = "webdav:innerProp";
   // outerPropNode.setProperty(innerProp, "innerValue");
   // session.save();
   //  
   // String xml = "" +
   // "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
   // "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\" xmlns:jcr=\"jcr:\">"
   // +
   // "<D:remove>" +
   // "<D:prop>" +
   // "<jcr:content>" +
   // "<webdav:outerProp>" +
   // "<webdav:innerProp/>" +
   // "</webdav:outerProp>" +
   // "</jcr:content>" +
   // "</D:prop>" +
   // "</D:remove>" +
   // "</D:propertyupdate>";
   //  
   // String path = node.getPath();
   // ContainerResponse response = service(WebDAVMethods.PROPPATCH, getPathWS() +
   // path, "", null, xml.getBytes());
   // assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
   //  
   // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   // PropPatchResponseEntity entity = (PropPatchResponseEntity)
   // response.getEntity();
   // entity.write(outputStream);
   //  
   // content = getContentNode(node);
   //  
   // try {
   // content.getNode(outerPropName).getProperty(innerProp);
   // fail();
   // } catch (Exception e) {
   // // Success there is no such property.
   // }
   //  
   // }

   public static Node getContentNode(Node node) throws RepositoryException
   {
      return node.getNode("jcr:content");
   }

}
