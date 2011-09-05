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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.transaction.TransactionService;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

/**
 * Created by The eXo Platform SAS. <br>
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestXATransaction.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestXATransaction extends JcrAPIBaseTest
{

   private TransactionService ts;

   public void setUp() throws Exception
   {

      super.setUp();

      ts = (TransactionService)container.getComponentInstanceOfType(TransactionService.class);

   }

   public void testSimpleGlobalTransaction() throws Exception
   {
      assertNotNull(ts);
      TransactionManager tm = ts.getTransactionManager();
      tm.begin();
      session.getRootNode().addNode("txg1");
      session.save();
      tm.commit();
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      assertNotNull(s1.getItem("/txg1"));

   }

   public void test2GlobalTransactions() throws Exception
   {
      assertNotNull(ts);
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      TransactionManager tm = ts.getTransactionManager();
      tm.begin();

      session.getRootNode().addNode("txg2");
      session.save();

      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      Transaction tx = tm.suspend();

      tm.begin();
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
      tm.commit();
      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      assertNotNull(s1.getItem("/txg3"));

      // Resume work with former transaction
      tm.resume(tx);

      // Commit work recorded when associated with xid2
      tm.commit();

      assertNotNull(s1.getItem("/txg2"));
      assertNotNull(s1.getItem("/txg3"));
      
      QueryManager manager = s1.getWorkspace().getQueryManager();
      Query query = manager.createQuery("select * from nt:base where jcr:path = '/txg2'", Query.SQL);
      QueryResult queryResult = query.execute();
      assertNotNull(queryResult);
      NodeIterator iter = queryResult.getNodes();
      assertEquals(1, iter.getSize());
      
      query = manager.createQuery("select * from nt:base where jcr:path = '/txg3'", Query.SQL);
      queryResult = query.execute();
      assertNotNull(queryResult);
      iter = queryResult.getNodes();
      assertEquals(1, iter.getSize());      
   }

   public void test2GlobalTransactions2() throws Exception
   {
      assertNotNull(ts);
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      TransactionManager tm = ts.getTransactionManager();
      tm.begin();

      session.getRootNode().addNode("txg2");
      session.save();

      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      Transaction tx = tm.suspend();

      tm.begin();
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
      tm.commit();
      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      assertNotNull(s1.getItem("/txg3"));

      // Resume work with former transaction
      tm.resume(tx);

      // Roll back work recorded when associated with xid2
      tm.rollback();
      
      try
      {
         s1.getItem("/txg2");
         fail("PathNotFoundException");
      }
      catch (PathNotFoundException e)
      {
      }
      assertNotNull(s1.getItem("/txg3"));
      
      QueryManager manager = s1.getWorkspace().getQueryManager();
      Query query = manager.createQuery("select * from nt:base where jcr:path = '/txg2'", Query.SQL);
      QueryResult queryResult = query.execute();
      assertNotNull(queryResult);
      NodeIterator iter = queryResult.getNodes();
      assertEquals(0, iter.getSize());
      
      query = manager.createQuery("select * from nt:base where jcr:path = '/txg3'", Query.SQL);
      queryResult = query.execute();
      assertNotNull(queryResult);
      iter = queryResult.getNodes();
      assertEquals(1, iter.getSize());
   }
   
   public void testLockInTransactions() throws LoginException, NoSuchWorkspaceException, RepositoryException,
      XAException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      assertNotNull(ts);
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      Session s2 =
         repository.login(new SimpleCredentials("exo", "exo".toCharArray()), session.getWorkspace().getName());

      Node n1 = session.getRootNode().addNode("testLock");
      n1.addMixin("mix:lockable");
      session.getRootNode().save();

      TransactionManager tm = ts.getTransactionManager();
      tm.begin();

      // lock node
      Lock lock = n1.lock(false, true);

      // assert: isLive must return true
      assertTrue("Lock must be live", lock.isLive());

      assertFalse(s2.getRootNode().getNode("testLock").isLocked());

      // Commit work recorded when associated with xid2
      tm.commit();
      assertTrue(s2.getRootNode().getNode("testLock").isLocked());

      n1.unlock();
   }
}
