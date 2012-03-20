/**
 * Copyright (C) 2010 eXo Platform SAS.
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
 *
 */

package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.tools.DummySecurityContext;
import org.exoplatform.services.rest.tools.ResourceLauncher;
import org.exoplatform.services.security.IdentityConstants;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;


/**
 * 
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public class TestAclCommand extends BaseStandaloneTest
{

   private final String USER_ONE = "Oksana";

   private final String USER_TWO = "Anya";

   private final String USER_ROOT = "root";

   private final String BASE_URI = "http://localhost";

   private final String TEST_NODE_NAME = "test_node" + System.currentTimeMillis();

   /**
    * Here we check for correct addition of privileges to users,
    * besides, we check for correct addition of mix:pribilegeable. 
    * @throws Exception
    */
   public void testSetACLForTwoUsersOnNonPrivilegeableResource() throws Exception
   {
      

      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_ONE + "</D:href>" + "</D:principal>" + "<D:grant>"
            + "<D:privilege><D:write/></D:privilege>" + "</D:grant>" + "</D:ace>" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_TWO + "</D:href>" + "</D:principal>" + "<D:grant>" + "<D:write/>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      checkPermissionSet(testNode, USER_ONE, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, USER_ONE, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, USER_ONE, PermissionType.REMOVE);

      checkPermissionSet(testNode, USER_TWO, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, USER_TWO, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, USER_TWO, PermissionType.REMOVE);

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct write permission removal from 
    * the mix:versionable and exo:privelegeable node. We add permissions manually
    * and then remove them via ACL method. After this operation they are expected
    * to be removed from node ACL
    * @throws Exception
    */
   public void testDenyPermissionOnPrivilegeableResource() throws Exception
   {

      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      testNode.addMixin("mix:versionable");
      testNode.addMixin("exo:privilegeable");
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.setPermission(USER_ONE, new String[]{"add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_ONE + "</D:href>" + "</D:principal>" + "<D:deny>" + "<D:privilege><D:write/></D:privilege>"
            + "</D:deny>" + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);

      checkPermissionRemoved(testNode, USER_ONE, PermissionType.ADD_NODE);
      checkPermissionRemoved(testNode, USER_ONE, PermissionType.SET_PROPERTY);
      checkPermissionRemoved(testNode, USER_ONE, PermissionType.REMOVE);

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct processing of knowingly malformed request.
    * We are trying to grant and deny the same privilege and expect
    * BAD_REQUEST status.
    * @throws Exception
    */
   public void testDenyAndGrantInASingleACE() throws Exception
   {

      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      testNode.addMixin("mix:versionable");
      testNode.addMixin("exo:privilegeable");
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_ONE + "</D:href>" + "</D:principal>" + "<D:deny>"
            + "<D:privilege><D:write/></D:privilege>" + "</D:deny>" + "<D:grant>"
            + "<D:privilege><D:write/></D:privilege>" + "</D:grant>" + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      testNode.remove();
      session.save();
   }

   /**
    * Here we test for correct setting all permissions for ANY user.
    * We create a node without all permissions for ANY user and expect
    * them to appear after receiving a response.
    * @throws Exception
    */
   public void testSetAllPermissionsForAllUsersOnPrivilegeableResource() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      session.save();

      Map<String, String[]> defaultPermissions = new HashMap<String, String[]>();
      String[] initPermissions =
         new String[]{PermissionType.ADD_NODE, PermissionType.READ, PermissionType.SET_PROPERTY};
      defaultPermissions.put(USER_TWO, initPermissions);
      testNode.setPermissions(defaultPermissions);
      session.save();

      System.out.println("Node before: " + testNode);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant>" + "<D:privilege><D:all/></D:privilege>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI,
            headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);

      System.out.println("Node after: " + testNode);

      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.REMOVE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.READ);

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct processing of knowingly malformed grant element
    * in ACL request body. 
    * @throws Exception
    */
   public void testWrongGrantElementAceElementInAclBody() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant>" + "<D:privilege><D:read /><D:write /></D:privilege>"
            + "</D:grant>" + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant></D:grant>" + "</D:ace>" + "</D:acl>";

      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant><D:privilege></D:privilege></D:grant>" + "</D:ace>"
            + "</D:acl>";

      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct processing of knowingly malformed ace element
    * in ACL request body. 
    * @throws Exception
    */
   public void testWrongAceElementInAclBody() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      session.save();


      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI,
            headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());
      
      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:grant>" + "<D:privilege><D:read /><D:write /></D:privilege>"
            + "</D:grant>" + "</D:ace>" + "</D:acl>";
      
      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI,
            headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());
      
      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
         + "<D:all />" + "</D:principal>" + "</D:ace>" + "</D:acl>";
      
      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI,
            headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      testNode.remove();
      session.save();
   }
   
   /**
    * Here we check for correct processing of knowingly malformed principal element
    * in ACL request body. 
    * @throws Exception
    */
   public void testWrongPrincipalElementInAclBody() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      session.save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "</D:principal>" + "<D:grant>" + "<D:privilege><D:read /><D:write /></D:privilege>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";

      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant>" + "<D:privilege><D:read /><D:write /></D:privilege>"
            + "</D:grant>" + "</D:ace>" + "</D:acl>";

      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + "</D:href>" + "</D:principal>" + "<D:grant>"
            + "<D:privilege><D:read /><D:write /></D:privilege>" + "</D:grant>" + "</D:ace>" + "</D:acl>";

      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_ONE + "</D:href>" + "<href>" + USER_TWO + "</href>" + "</D:principal>" + "<D:grant>"
            + "<D:privilege><D:read /><D:write /></D:privilege>" + "</D:grant>" + "</D:ace>" + "</D:acl>";

      response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.BAD_REQUEST, response.getStatus());

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct ACL setting for mix:versionable, exo:owneable, exo:privilegeable,
    * checked out node. Node is manually set to have any permission for user "root". Node expected 
    * to have any permission for any user
    * after ACL method completion.  
    * @throws Exception
    */
   public void testSetAclForVersionableOwneablePrivilegeableCheckedOutNode() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      testNode.addMixin("mix:versionable");
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      //let us make node version
      testNode.checkin();
      testNode.checkout();

      session.save();

      //now let us try to grant all permissions for any user
      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant>" + "<D:privilege><D:all/></D:privilege>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";
      
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);
      
      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);
      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);
      
      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);

      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.REMOVE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.READ);

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct ACL setting for mix:versionable, checkedin node. Node is manually set 
    * to have any permission for user USER_ONE. Node expected to get checked out, added corresponding mixins, 
    * and to set any permission for any user.
    * after ACL method completion.  
    * @throws Exception
    */
   public void testSetAclForVersionableCheckedInNode() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();
      testNode.addMixin("mix:versionable");
      session.save();

      //let us make node version
      testNode.checkin();
      testNode.checkout();
      testNode.checkin();
      session.save();

      //now let us try to grant all permissions for user USER_ONE
      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:href>" + USER_ONE + "</D:href>" + "</D:principal>" + "<D:grant>"
            + "<D:privilege><D:all/></D:privilege>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);
      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);

      checkPermissionSet(testNode, USER_ONE, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, USER_ONE, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, USER_ONE, PermissionType.REMOVE);
      checkPermissionSet(testNode, USER_ONE, PermissionType.READ);

      testNode.remove();
      session.save();
   }

   /**
    * Here we check for correct ACL setting for mix:versionable, exo:owneable, exo:privilegeable,
    * checked in node. Node is manually set to have any permission for user "root". Node expected 
    * to have any permission for any user
    * after ACL method completion.  
    * @throws Exception
    */
   public void testSetAclForVersionableOwneablePrivilegeableCheckedInNode() throws Exception
   {
      NodeImpl testNode = (NodeImpl)root.addNode(TEST_NODE_NAME, "nt:folder");
      session.save();
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      testNode.addMixin("mix:versionable");
      testNode.setPermission(USER_ROOT, new String[]{"read", "add_node", "set_property", "remove"});
      testNode.removePermission(IdentityConstants.ANY);
      session.save();

      //let us make node version
      testNode.checkin();
      testNode.checkout();
      testNode.checkin();

      session.save();

      //now let us try to grant all permissions for any user
      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:acl xmlns:D=\"DAV:\">" + "<D:ace>" + "<D:principal>"
            + "<D:all />" + "</D:principal>" + "<D:grant>" + "<D:privilege><D:all/></D:privilege>" + "</D:grant>"
            + "</D:ace>" + "</D:acl>";

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);
      ContainerResponse response =
         launcher.service(WebDavConstants.WebDAVMethods.ACL, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.OK, response.getStatus());

      session.refresh(false);
      testNode = (NodeImpl)root.getNode(TEST_NODE_NAME);

      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.ADD_NODE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.SET_PROPERTY);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.REMOVE);
      checkPermissionSet(testNode, IdentityConstants.ANY, PermissionType.READ);

      testNode.remove();
      session.save();
   }

   private void checkPermissionSet(NodeImpl node, String identity, String permission) throws RepositoryException
   {
      for (AccessControlEntry entry : node.getACL().getPermissionEntries())
      {
         if (entry.getIdentity().equals(identity) && entry.getPermission().equals(permission))
         {
            return;
         }
      }

      fail();
   }

   private void checkPermissionRemoved(NodeImpl node, String identity, String permission) throws RepositoryException
   {
      for (AccessControlEntry entry : node.getACL().getPermissionEntries())
      {
         if (entry.getIdentity().equals(identity) && entry.getPermission().equals(permission))
         {
            fail();
         }
      }
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
