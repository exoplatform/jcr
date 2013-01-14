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
 */

package org.exoplatform.services.jcr.impl.core.security;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TesterItemsPersistenceListener;
import org.exoplatform.services.security.IdentityConstants;

import java.security.AccessControlException;
import java.security.PrivilegedExceptionAction;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: TestGetSystemSession.java 2521 2010-06-09 11:50:54Z nzamosenchuk $
 */
public class TestSecurityRepositoryManagment extends BaseSecurityTest
{
   public void testGetSystemSessionSuccess()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getSystemSession();
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able get system session. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testGetSystemSessionFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getSystemSession();
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able get system session.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testGetSystemSessionFail2()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.login(new CredentialsImpl(IdentityConstants.SYSTEM, "".toCharArray()), repository.getSystemWorkspaceName());
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able get system session.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }
   public void testAddItemPersistenceListenerSuccess()
   {
      final TesterItemsPersistenceListener listener = new TesterItemsPersistenceListener(session, false);
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.addItemPersistenceListener(workspace.getName(), listener);
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able add listener. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
      finally
      {
         // unregister the listener to avoid keeping ItemData into the memory for nothing
         listener.pushChanges();
      }
   }

   public void testAddItemPersistenceListenerFail()
   {
      final TesterItemsPersistenceListener listener = new TesterItemsPersistenceListener(session, false);
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.addItemPersistenceListener(workspace.getName(), listener);
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able add listener.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
      finally
      {
         // unregister the listener to avoid keeping ItemData into the memory for nothing
         listener.pushChanges();
      }
   }

   public void testConfigWorkspaceSuccess() throws Exception
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            WorkspaceEntry defConfig =
               (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

            WorkspaceEntry wsConfig = new WorkspaceEntry();
            wsConfig.setName("testCWS");

            wsConfig.setAccessManager(defConfig.getAccessManager());
            wsConfig.setCache(defConfig.getCache());
            wsConfig.setContainer(defConfig.getContainer());
            wsConfig.setLockManager(defConfig.getLockManager());

            repository.configWorkspace(wsConfig);
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able config workspace. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }

      action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            // remove configured workspace
            repository.createWorkspace("testCWS");
            repository.internalRemoveWorkspace("testCWS");
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able config workspace. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testConfigWorkspaceFail() throws Exception
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            WorkspaceEntry defConfig =
               (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

            WorkspaceEntry wsConfig = new WorkspaceEntry();
            wsConfig.setName("testConfigWorkspaceFail");

            wsConfig.setAccessManager(defConfig.getAccessManager());
            wsConfig.setCache(defConfig.getCache());
            wsConfig.setContainer(defConfig.getContainer());
            wsConfig.setLockManager(defConfig.getLockManager());

            repository.configWorkspace(wsConfig);
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able config workspace.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testCreateWorkspaceSuccess() throws Exception
   {
      // configures workspace for creation
      WorkspaceEntry defConfig =
         (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      final WorkspaceEntry wsConfig = new WorkspaceEntry();
      wsConfig.setName("testCWS");

      wsConfig.setAccessManager(defConfig.getAccessManager());
      wsConfig.setCache(defConfig.getCache());
      wsConfig.setContainer(defConfig.getContainer());
      wsConfig.setLockManager(defConfig.getLockManager());

      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.configWorkspace(wsConfig);
            repository.createWorkspace("testCWS");
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able create workspace. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
      action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            // remove configured workspace
            repository.internalRemoveWorkspace("testCWS");
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able config workspace. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }

   }

   public void testCreateWorkspaceFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.createWorkspace("testCreateWorkspaceFail");
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able create workspace.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testInternalRemoveWorkspaceSuccess() throws Exception
   {
      // configures and create workspace
      WorkspaceEntry defConfig =
         (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      final WorkspaceEntry wsConfig = new WorkspaceEntry();
      wsConfig.setName("testIRWS");

      wsConfig.setAccessManager(defConfig.getAccessManager());
      wsConfig.setCache(defConfig.getCache());
      wsConfig.setContainer(defConfig.getContainer());
      wsConfig.setLockManager(defConfig.getLockManager());


      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.configWorkspace(wsConfig);
            repository.createWorkspace("testIRWS");
            repository.internalRemoveWorkspace("testIRWS");
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able remove workspace. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testInternalRemoveWorkspaceFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.internalRemoveWorkspace("testInternalRemoveWorkspaceFail");
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able remove workspace.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testSetStateeSuccess()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.setState(ManageableRepository.OFFLINE);
            repository.setState(ManageableRepository.ONLINE);
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able set state. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testSetStateFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.setState(ManageableRepository.OFFLINE);
            repository.setState(ManageableRepository.ONLINE);
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able set state.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testGetConfigurationSuccess()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getConfiguration();
            return null;
         }

      };
      try
      {
         doPrivilegedActionStaticPermissions(action);
      }
      catch (AccessControlException ace)
      {
         fail("Must be able get configuration. We are under static permissions");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

   public void testGetConfigurationFail()
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            repository.getConfiguration();
            return null;
         }

      };
      try
      {
         doPrivilegedAction(action);
         fail("Must not be able get configuration.");
      }
      catch (AccessControlException ace)
      {
         // OK
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         fail();
      }
   }

}
