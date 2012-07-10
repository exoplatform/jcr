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
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.transaction.TransactionService;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;

/**
 * Created by The eXo Platform SAS. <br>
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestUserTransaction.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestUserTransaction extends JcrAPIBaseTest
{

   private TransactionService txService;

   public void setUp() throws Exception
   {
      super.setUp();
      txService = (TransactionService)container.getComponentInstanceOfType(TransactionService.class);
   }

   private List<Session> openSomeSessions() throws Exception
   {
      assertNotNull(txService);
      List<Session> someSessions = new ArrayList<Session>();

      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      Node rootS1 = s1.getRootNode();
      rootS1.addNode("someNode1");
      rootS1.save();
      someSessions.add(s1);
      Session s2 =
         repository.login(new SimpleCredentials("exo", "exo".toCharArray()), session.getWorkspace().getName());
      Node rootS2 = s2.getRootNode();
      rootS2.addNode("someNode2");
      rootS2.save();
      someSessions.add(s2);
      Session s3 =
         repository.login(new SimpleCredentials("exo", "exo".toCharArray()), session.getWorkspace().getName());
      Node rootS3 = s3.getRootNode();
      rootS3.addNode("someNode3");
      rootS3.getNode("someNode2").remove();
      rootS3.save();
      someSessions.add(s3);
      Session s4 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      Node rootS4 = s4.getRootNode();
      Node n = rootS4.getNode("someNode3");
      n.addNode("someNode4");
      rootS4.getNode("someNode1").remove();
      rootS4.save();
      someSessions.add(s4);

      // some logouts
      session.logout();
      someSessions.add(session);
      s1.logout();

      // ...from setUp()
      session = (SessionImpl)repository.login(credentials, "ws");
      someSessions.add(session);

      workspace = session.getWorkspace();
      root = session.getRootNode();
      valueFactory = session.getValueFactory();

      return someSessions;
   }

   public void testCommit() throws Exception
   {
      assertNotNull(txService);
      List<Session> someSessions = openSomeSessions();
      UserTransaction ut = txService.getUserTransaction();
      ut.begin();
      // we need to create the session within the transaction to ensure that it will be enlisted
      Session session = repository.login(credentials, "ws");
      session.getRootNode().addNode("txcommit");
      session.save();
      assertNotNull(session.getItem("/txcommit"));
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      try
      {
         assertNotNull(s1.getItem("/txcommit"));
         fail("PathNotFoundException should have be thrown");
      }
      catch (PathNotFoundException e)
      {
         //ok
      }
      ut.commit();
      assertNotNull(s1.getItem("/txcommit"));

      someSessions.clear();
   }

   public void testRollback() throws Exception
   {
      assertNotNull(txService);
      UserTransaction ut = txService.getUserTransaction();

      ut.begin();
      // we need to create the session within the transaction to ensure that it will be enlisted
      Session session = repository.login(credentials, "ws");
      session.getRootNode().addNode("txrollback");
      session.save();
      assertNotNull(session.getItem("/txrollback"));

      // Session s1 = repository.login(new SimpleCredentials("admin","admin".toCharArray()),
      // session.getWorkspace().getName());
      ut.rollback();
      try
      {
         assertNotNull(session.getItem("/txrollback"));
         fail("PathNotFoundException should have be thrown");
      }
      catch (PathNotFoundException e)
      {
      }
   }

   // we don't have JNID for JBossTS in standalone now  
   public void _testUserTransactionFromJndi() throws Exception
   {
      assertNotNull(txService);

      InitialContext ctx = new InitialContext();
      Object obj = ctx.lookup("UserTransaction");
      UserTransaction ut = (UserTransaction)obj;

      ut.begin();
      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());
      s1.getRootNode().addNode("txcommit1");
      s1.save();
      ut.commit();
      assertNotNull(session.getItem("/txcommit1"));
   }

   public void testReuseUT() throws Exception
   {
      assertNotNull(txService);
      // TODO in JNDI only JOTM today
      //InitialContext ctx = new InitialContext();
      //Object obj = ctx.lookup("UserTransaction");
      //UserTransaction ut = (UserTransaction)obj;

      UserTransaction ut = txService.getUserTransaction();

      ut.begin();

      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      Node tx2 = s1.getRootNode().addNode("txcommit2");
      ut.commit();

      ut.begin();

      tx2.addNode("txcommit21");
      s1.save();
      ut.commit();
      assertNotNull(session.getItem("/txcommit2/txcommit21"));

   }

   public void testAddRemoveNode() throws Exception
   {
      UserTransaction ut = txService.getUserTransaction();

      root.addNode("Node");
      session.save();

      ut.begin();
      Node node = root.getNode("Node");
      String uuid = ((NodeImpl)node).getIdentifier();
      node.setProperty("name", "value");

      session.save();

      node.remove();
      session.save();

      ItemData itemData = session.getTransientNodesManager().getItemData(uuid);
      boolean hasItemData = session.getTransientNodesManager().hasItemData((NodeData)((NodeImpl)root).getData(), QPathEntry.parse("[]Node:1"),
         ItemType.NODE);
      ut.commit();

      assertNull(itemData);
      assertFalse(hasItemData);
   }

   public void testSaveException() throws Exception
   {
      assertNotNull(txService);
      // TODO in JNDI only JOTM today
      //InitialContext ctx = new InitialContext();
      //Object obj = ctx.lookup("UserTransaction");
      //UserTransaction ut = (UserTransaction)obj;

      UserTransaction ut = txService.getUserTransaction();

      Session s0 =
         repository.login(new SimpleCredentials("root", "exo".toCharArray()), session.getWorkspace().getName());
      Node pretx = s0.getRootNode().addNode("pretx");
      s0.save();

      pretx.remove(); // don't save now

      ut.begin();

      Session s1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      Session s2 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), session.getWorkspace().getName());

      Node tx1 = s1.getRootNode().getNode("pretx").addNode("tx1");

      Node tx2 = s2.getRootNode().getNode("pretx").addNode("tx2");

      // keep this change out of the current Tx, this is necessary since
      // we have now auto-enlistment mechanism that is triggered at session save
      Transaction tx = txService.getTransactionManager().suspend();
      s0.save(); // save that parent of tx1 removed
      txService.getTransactionManager().resume(tx);

      s1.save();
      s2.save();

      try
      {
         ut.commit();
         // internally XAException should be thrown
         fail("Exception should occurs");
      }
      catch (Exception e)
      {
         // ok
      }

      s1.logout();
      s2.logout();

      try
      {
         session.getItem("/pretx/tx1");
         fail("PathNotFoundException should be thrown");
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testSetRollbackOnly() throws Exception
   {
      assertNotNull(txService);
      UserTransaction ut = txService.getUserTransaction();
      ut.begin();
      // we need to create the session within the transaction to ensure that it will be enlisted
      Session session = repository.login(credentials, "ws");
      session.getRootNode().addNode("txrollback");

      ut.setRollbackOnly();
      try
      {
         session.save();
         fail("IllegalStateException should have be thrown as a save is forbidden after a setRollbackOnly");
      }
      catch (IllegalStateException e)
      {
      }
      
      try
      {
         assertNotNull(session.getItem("/txrollback"));
         fail("PathNotFoundException should have be thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      
      ut.rollback();
      try
      {
         assertNotNull(session.getItem("/txrollback"));
         fail("PathNotFoundException should have be thrown");
      }
      catch (PathNotFoundException e)
      {
      }
   }
}
