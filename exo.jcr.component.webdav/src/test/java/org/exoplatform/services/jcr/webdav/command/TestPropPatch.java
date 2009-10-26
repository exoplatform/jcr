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
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayOutputStream;
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

   private final String patchRemove =
      "<?xml version=\"1.0\"?><D:propertyupdate xmlns:D=\"DAV:\" xmlns:b=\"urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/\" xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\"><D:remove><D:prop><webdav:Author/></D:prop></D:remove></D:propertyupdate>";

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   // public void testPropPatchSet() throws Exception {
   // String content = TestUtils.getFileContent();
   // ByteArrayInputStream inputStream = new
   // ByteArrayInputStream(content.getBytes());
   // String file = TestUtils.getFileName();
   // TestUtils.addContent(session, file, inputStream, nt_webdave_file, "");
   // ContainerResponse patchSet = service(WebDAVMethods.PROPPATCH,
   // getPathWS() + file,
   // "",
   // null,
   // patch.getBytes());
   // assertEquals(HTTPStatus.MULTISTATUS, patchSet.getStatus());
   // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   // PropPatchResponseEntity entity = (PropPatchResponseEntity)
   // patchSet.getEntity();
   // entity.write(outputStream);
   // Property prop = TestUtils.getNodeProperty(session, file, authorProp);
   // assertNotNull(prop);
   // assertEquals(prop.getString(), author);
   // }
   //
   // public void testPropPatchRemove() throws Exception {
   // String content = TestUtils.getFileContent();
   // ByteArrayInputStream inputStream = new
   // ByteArrayInputStream(content.getBytes());
   // String file = TestUtils.getFileName();
   // TestUtils.addContent(session, file, inputStream, nt_webdave_file, "");
   // TestUtils.addNodeProperty(session, file, authorProp, author);
   // Property prop = TestUtils.getNodeProperty(session, file, authorProp);
   // assertNotNull(prop);
   // assertEquals(prop.getString(), author);
   // ContainerResponse responceRemove = service(WebDAVMethods.PROPPATCH,
   // getPathWS() + file,
   // "",
   // null,
   // patchRemove.getBytes());
   // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   // PropPatchResponseEntity entity = (PropPatchResponseEntity)
   // responceRemove.getEntity();
   // entity.write(outputStream);
   // assertEquals(HTTPStatus.MULTISTATUS, responceRemove.getStatus());
   // prop = TestUtils.getNodeProperty(session, file, authorProp);
   // assertNull(prop);
   // }
   //
   // public void testPropPatchSetWithLock() throws Exception {
   // String content = TestUtils.getFileContent();
   // ByteArrayInputStream inputStream = new
   // ByteArrayInputStream(content.getBytes());
   // String file = TestUtils.getFileName();
   // TestUtils.addContent(session, file, inputStream, nt_webdave_file, "");
   // TestUtils.lockNode(session, file, true);
   // ContainerResponse patchSet = service(WebDAVMethods.PROPPATCH,
   // getPathWS() + file,
   // "",
   // null,
   // patch.getBytes());
   // assertEquals(HTTPStatus.MULTISTATUS, patchSet.getStatus());
   // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   // PropPatchResponseEntity entity = (PropPatchResponseEntity)
   // patchSet.getEntity();
   // entity.write(outputStream);
   // Property prop = TestUtils.getNodeProperty(session, file, authorProp);
   // assertNotNull(prop);
   // assertEquals(prop.getString(), author);
   // }

   public void testPropPatchSetWithLock2() throws Exception
   {

      String fileContent = TestUtils.getFileContent();

      String fileName = "testPropPatchFile";

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

      ContainerResponse response =
         service(WebDAVMethods.PROPPATCH, getPathWS() + "/" + fileName, "", null, patch.getBytes());
      PropPatchResponseEntity entity = (PropPatchResponseEntity)response.getEntity();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();

      Property p = node.getProperty(authorProp);
      String str = p.getString();
      str.trim();

   }

   // public void testPropPatch() throws Exception {
   // String description = "test description property";
   // String rights = "test rights property";
   // Node propPatchNode = session.getRootNode().addNode("propPatchNode",
   // "nt:unstructured");
   // propPatchNode.addMixin("dc:elementSet");
   // session.save();
   // String path = propPatchNode.getPath();
   // String xml = "" + "<D:propertyupdate xmlns:D=\"DAV:\">" + "<D:set>" +
   // "<D:prop>"
   // + "<D:contentlength>10</D:contentlength>" +
   // "<D:someprop>somevalue</D:someprop>"
   // + "<dc:description xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
   // description
   // + "</dc:description>" +
   // "<dc:rights xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
   // + rights + "</dc:rights>" + "</D:prop>" + "</D:set>" + "<D:remove>" +
   // "<D:prop>"
   // + "<D:prop2 />" + "<D:prop3 />" + "</D:prop>" + "</D:remove>" +
   // "</D:propertyupdate>";
   //
   // ContainerResponse response = service(WebDAVMethods.PROPPATCH,
   // getPathWS() + path,
   // "",
   // null,
   // xml.getBytes());
   // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   // PropPatchResponseEntity entity = (PropPatchResponseEntity)
   // response.getEntity();
   // entity.write(outputStream);
   // String resp = outputStream.toString();
   // HierarchicalPropertyEntityProvider entityProvider = new
   // HierarchicalPropertyEntityProvider();
   // HierarchicalProperty multistatus = entityProvider.readFrom(null,
   // null,
   // null,
   // null,
   // null,
   // new ByteArrayInputStream(resp.getBytes()));
   // assertEquals(new QName("DAV:", "multistatus"), multistatus.getName());
   // assertEquals(1, multistatus.getChildren().size());
   // assertEquals(new QName("DAV:", "response"),
   // multistatus.getChild(0).getName());
   // Map<QName, WebDavProperty> properties =
   // XmlUtils.parsePropStat(multistatus.getChild(0));
   // WebDavProperty descriptionProp = properties.get(new
   // QName("http://purl.org/dc/elements/1.1/",
   // "description",
   // "dc"));
   // assertNotNull(descriptionProp);
   // assertEquals(HTTPStatus.OK, descriptionProp.getStatus());
   // WebDavProperty rightsProp = properties.get(new
   // QName("http://purl.org/dc/elements/1.1/",
   // "rights",
   // "dc"));
   // assertNotNull(rightsProp);
   // assertEquals(HTTPStatus.OK, rightsProp.getStatus());
   // propPatchNode = session.getRootNode().getNode("propPatchNode");
   // Value[] values = propPatchNode.getProperty("dc:description").getValues();
   // String desccriptionGet = values[0].getString();
   // values = propPatchNode.getProperty("dc:rights").getValues();
   // String rightsGet = values[0].getString();
   // assertEquals(desccriptionGet, description);
   // assertEquals(rightsGet, rights);
   // }

}
