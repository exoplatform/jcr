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
package org.exoplatform.services.jcr.api.accessing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created y the eXo platform team
 * 
 * @author Benjamin Mestrallet
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestAccessRepository.java 12049 2008-03-18 12:22:03Z gazarenkov $ See 6.1.1
 */
public class TestAccessRepository extends JcrAPIBaseTest
{

   public void testLogin() throws NoSuchWorkspaceException, LoginException, RepositoryException
   {
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session session = repository.login(cred, WORKSPACE);
      assertNotNull(session);
      assertEquals("exo", session.getUserID());
      assertEquals(WORKSPACE, session.getWorkspace().getName());
   }

   /**
    * 6.1.2 If credentials is null, it is assumed that authentication is handled by a mechanism
    * external to the repository itself (for example, through the JAAS framework) and that the
    * repository implementation exists within a context (for example, an application server) that
    * allows it to handle authorization of the request for access to the specified workspace
    */
   public void testExternalLogin() throws NoSuchWorkspaceException, LoginException, RepositoryException
   {

      // make sure for this thread the user is "exo"
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session session1 = repository.login(cred, WORKSPACE);

      // new session with the same credentials
      Session session = repository.login(WORKSPACE);
      assertNotNull(session);
      assertEquals("exo", session.getUserID());
      assertEquals(WORKSPACE, session.getWorkspace().getName());
      assertFalse(session1.equals(session));
   }

   /**
    * 6.1.2 If workspaceName is null, a default workspace is automatically selected by the repository
    * implementation. This may, for example, be the �home workspace� of the user whose
    * credentials were passed, though this is entirely up to the configuration and implementation of
    * the repository. Alternatively it may be a null workspace that serves only to provide the method
    */
   public void testLoginToDefaultWorkspace() throws NoSuchWorkspaceException, LoginException, RepositoryException
   {
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session session = repository.login(cred);
      assertNotNull(session);
      assertEquals(WORKSPACE, session.getWorkspace().getName());
   }

   public void testExternalLoginToDefaultWorkspace() throws NoSuchWorkspaceException, LoginException,
      RepositoryException
   {

      // PortalContainer.getInstance().
      // createSessionContainer("my-session", "gena").
      // setClientInfo(new DummyClientInfo("gena"));
      // container.createSessionContainer("my-session", "gena").
      // setClientInfo(new DummyClientInfo("gena"));

      Session session = repository.login();
      assertNotNull(session);
      assertEquals(WORKSPACE, session.getWorkspace().getName());
   }

   public void testWrongLogin() throws Exception
   {
      Credentials cred = new CredentialsImpl("benj", "".toCharArray());
      try
      {
         repository.login(cred, WORKSPACE);
         fail("Exception should have been thrown");
      }
      catch (LoginException e)
      {
      }
   }

   public void testWrongWorkspace() throws Exception
   {
      Credentials cred = this.credentials;
      // new CredentialsImpl("user", "psw".toCharArray());
      try
      {
         repository.login(cred, "wrong_workspace");
         fail("Exception should have been thrown");
      }
      catch (NoSuchWorkspaceException e)
      {
      }
   }

   public void testDescriptors() throws Exception
   {
      assertTrue(repository.getDescriptorKeys().length >= 6);
      assertNotNull(repository.getDescriptor(Repository.REP_NAME_DESC));
   }
}
