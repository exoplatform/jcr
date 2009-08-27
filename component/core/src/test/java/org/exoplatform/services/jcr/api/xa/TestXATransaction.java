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
package org.exoplatform.services.jcr.api.xa;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.Lock;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.XASession;
import org.exoplatform.services.transaction.TransactionService;

/**
 * Created by The eXo Platform SAS. <br>
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestXATransaction.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestXATransaction
   extends JcrAPIBaseTest
{

   private TransactionService ts;

   public void setUp() throws Exception
   {

      super.setUp();

      ts = (TransactionService) container.getComponentInstanceOfType(TransactionService.class);

   }

   public void testSimpleGlobalTransaction() throws Exception
   {
      assertNotNull(ts);
      Xid id = ts.createXid();
      XAResource xares = ((XASession) session).getXAResource();
      xares.start(id, XAResource.TMNOFLAGS);
      session.getRootNode().addNode("txg1");
      session.save();
      xares.commit(id, true);
      Session s1 =
               repository
                        .login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      assertNotNull(s1.getItem("/txg1"));

   }

   public void test2GlobalTransactions() throws Exception
   {
      assertNotNull(ts);
      Session s1 =
               repository
                        .login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      Xid id1 = ts.createXid();
      XAResource xares = ((XASession) session).getXAResource();
      xares.start(id1, XAResource.TMNOFLAGS);

      session.getRootNode().addNode("txg2");
      session.save();
      // xares.commit(id, true);
      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      xares.end(id1, XAResource.TMSUSPEND);

      Xid id2 = ts.createXid();
      xares.start(id2, XAResource.TMNOFLAGS);
      session.getRootNode().addNode("txg3");
      session.save();

      try
      {
         s1.getItem("/txg3");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }

      // End work
      xares.end(id2, XAResource.TMSUCCESS);

      // Resume work with former transaction
      xares.start(id1, XAResource.TMRESUME);

      // Commit work recorded when associated with xid2
      xares.commit(id1, true);
      // xares.commit(id2, true);
      assertNotNull(s1.getItem("/txg2"));
      assertNotNull(s1.getItem("/txg3"));

   }

   public void testLockInTransactions() throws LoginException, NoSuchWorkspaceException, RepositoryException,
            XAException
   {
      assertNotNull(ts);
      Session s1 =
               repository
                        .login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      Session s2 =
               repository.login(new SimpleCredentials("exo", "exo".toCharArray()), session.getWorkspace().getName());

      Node n1 = session.getRootNode().addNode("testLock");
      n1.addMixin("mix:lockable");
      session.getRootNode().save();

      Xid id1 = ts.createXid();
      XAResource xares = ((XASession) session).getXAResource();
      xares.start(id1, XAResource.TMNOFLAGS);

      // lock node
      Lock lock = n1.lock(false, true);

      // assert: isLive must return true
      assertTrue("Lock must be live", lock.isLive());

      assertFalse(s2.getRootNode().getNode("testLock").isLocked());

      // End work
      xares.end(id1, XAResource.TMSUCCESS);

      // Commit work recorded when associated with xid2
      xares.commit(id1, true);
      assertTrue(s2.getRootNode().getNode("testLock").isLocked());

      n1.unlock();
   }
}
