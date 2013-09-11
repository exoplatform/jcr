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
import org.exoplatform.services.jcr.webdav.command.LockCommand.LockResultResponseEntity;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 10 Dec 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: TestLock.java
 */

public class TestLock extends BaseStandaloneTest
{

   private String path = TestUtils.getFileName();

   private String fileContent = TestUtils.getFileContent();

   private String lockRequestBody = "<D:lockinfo xmlns:D='DAV:'>" + "<D:lockscope>" + "<D:exclusive/>"
      + "</D:lockscope>" + "<D:locktype>" + "<D:write/>" + "</D:locktype>" + "<D:owner>" + "<D:href>testOwner</D:href>"
      + "</D:owner>" + "</D:lockinfo>";

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      session.getRootNode().addNode(TextUtil.relativizePath(path));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
      path = path + "[2]";
   }

   /**
    * Testing Lock method. Firstly lock existing node via webdav LOCK method and check for correct response status.
    * Secondly check if the node is locked indeed via webdav DELETE method (no delete or any other operation cannot
    * be performed with locked node). The response status must be LOCKED.
    * @throws Exception
    */
   public void testLock() throws Exception
   {
      testLock(getPathWS());
   }

   /**
    * Same test as the previous one but with a path of the workspace containing
    * an incorrect repository name. The behavior should not change as we ignore it and use
    * the current repository.
    */
   public void testLockWithFakePathWS() throws Exception
   {
      testLock(getFakePathWS());
   }

   /**
    * Testing Lock method. Firstly lock existing node via webdav LOCK method and check for correct response status.
    * Secondly check if the node is locked indeed via webdav DELETE method (no delete or any other operation cannot
    * be performed with locked node). The response status must be LOCKED.
    * @param pathWs path of the workspace 
    * @throws Exception
    */
   private void testLock(String pathWs) throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENTTYPE, MediaType.TEXT_PLAIN);
      ContainerResponse containerResponse =
               serviceWithEscape(WebDAVMethods.LOCK, pathWs + path, "", headers, lockRequestBody.getBytes());

      assertEquals(HTTPStatus.OK, containerResponse.getStatus());

      // some manipulation to serialize response entity
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      LockResultResponseEntity entity = (LockResultResponseEntity)containerResponse.getEntity();
      entity.write(outputStream);

      containerResponse = serviceWithEscape("DELETE", pathWs + path, "", null, null);

      assertEquals(HTTPStatus.LOCKED, containerResponse.getStatus());
      assertTrue(session.getRootNode().getNode(TextUtil.relativizePath(path)).isLocked());

      // here we're unlocking a node, to use it in other tests
      // firstly get lock-token from response entity
      String lockToken = outputStream.toString();
      lockToken = lockToken.substring(lockToken.indexOf(">opaquelocktoken:"));
      lockToken = lockToken.substring(lockToken.indexOf(":") + 1, lockToken.indexOf("<"));

      // secondly add lock-token to current session and unlock the node 
      session.addLockToken(lockToken);
      ((Node)session.getItem(path)).unlock();
   }

   /**
    * Testing lock refreshing. Firstly we lock the node. Secondly we refresh the lock
    * via webdav LOCK method with no body. In both cases OK webdav status must be returned. 
    * @throws Exception
    */
   public void testLockRefresh() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add("Content-Type", MediaType.TEXT_PLAIN);
      ContainerResponse containerResponse =
         serviceWithEscape(WebDAVMethods.LOCK, getPathWS() + path, "", headers, lockRequestBody.getBytes());

      assertEquals(HTTPStatus.OK, containerResponse.getStatus());

      // get lock-token from response body
      // some manipulation to serialize response entity
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      LockResultResponseEntity entity = (LockResultResponseEntity)containerResponse.getEntity();
      entity.write(outputStream);

      String lockToken = outputStream.toString();
      lockToken = lockToken.substring(lockToken.indexOf(">opaquelocktoken:"));
      lockToken = lockToken.substring(lockToken.indexOf(":") + 1, lockToken.indexOf("<"));

      //prepare to send lock refresh request
      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENTTYPE, MediaType.TEXT_PLAIN);
      headers.add(ExtHttpHeaders.LOCKTOKEN, lockToken);

      containerResponse = serviceWithEscape(WebDAVMethods.LOCK, getPathWS() + path, "", headers, null);

      assertEquals(HTTPStatus.OK, containerResponse.getStatus());

      // some manipulation to serialize response entity
      outputStream = new ByteArrayOutputStream();
      entity = (LockResultResponseEntity)containerResponse.getEntity();
      entity.write(outputStream);

      // add lock-token to current session and unlock the node 
      session.addLockToken(lockToken);
      ((Node)session.getItem(path)).unlock();

   }

   /**
    * Testing trying to lock already locked node. Firstly we lock the node. Secondly we send webdav LOCK
    * request with request body (because in case the body is empty the LOCK request should refresh already
    * existing lock) to try to lock previously locked node. LOCKED webdav status must be returned.
    * @throws Exception
    */
   public void testAlreadyLocked() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENTTYPE, MediaType.TEXT_PLAIN);
      ContainerResponse containerResponse =
         serviceWithEscape(WebDAVMethods.LOCK, getPathWS() + path, "", headers, lockRequestBody.getBytes());

      assertEquals(HTTPStatus.OK, containerResponse.getStatus());

      // some manipulation to serialize response entity
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      LockResultResponseEntity entity = (LockResultResponseEntity)containerResponse.getEntity();
      entity.write(outputStream);

      String lockToken = outputStream.toString();
      lockToken = lockToken.substring(lockToken.indexOf(">opaquelocktoken:"));
      lockToken = lockToken.substring(lockToken.indexOf(":") + 1, lockToken.indexOf("<"));

      // prepare to send lock request
      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENTTYPE, MediaType.TEXT_PLAIN);
      containerResponse = serviceWithEscape(WebDAVMethods.LOCK, getPathWS() + path, "", headers, lockRequestBody.getBytes());

      assertEquals(HTTPStatus.LOCKED, containerResponse.getStatus());

      // add lock-token to current session and unlock the node
      session.addLockToken(lockToken);
      ((Node)session.getItem(path)).unlock();
   }

   /**
    * Here we're testing the case when we are trying to lock a resource C at a path /A/B/C
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testLockForNonExistingWorkspace() throws Exception
   {
      ContainerResponse response =
         serviceWithEscape(WebDAVMethods.LOCK, getPathWS() + "_" + path, "", null, lockRequestBody.getBytes());

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
