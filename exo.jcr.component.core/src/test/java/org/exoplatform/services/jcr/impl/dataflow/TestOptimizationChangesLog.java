/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TesterItemsPersistenceListener;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2012
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestOptimizationChangesLog.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class TestOptimizationChangesLog extends JcrImplBaseTest
{
   /**
    * Use case #1 :
    *    setProperty("x", "a");
    *    setProperty("x", "b");
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", "c"); ADDED
    */
   public void testAddAddAdd() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session); 
     
     testNode.setProperty("x", "a");
     testNode.setProperty("x", "b");
     testNode.setProperty("x", "c");
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges(); 
     
     assertEquals(1, logs.size());
     
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(1, states.size());

     assertEquals(ItemState.ADDED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
   }
   
   /**
    * Use case #2 :
    *    setProperty("x", "a");
    *    save()
    *    setProperty("x", "b");
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", "c"); UPDATED
    */
   public void testAddSaveAddAdd() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     testNode.setProperty("x", "a");
     session.save();
     
     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);
     
     testNode.setProperty("x", "b");
     testNode.setProperty("x", "c");
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges(); 
     
     assertEquals(1, logs.size());
     
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(1, states.size());

     assertEquals(ItemState.UPDATED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
   }
   
   /**
    * Use case #3 :
    *    setProperty("x", "a");
    *    setProperty("x", "b");
    *    setProperty("x", null);
    * Expected persistent changes :
    *    no changes
    */
   public void testAddAddDel() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session); 
     
     testNode.setProperty("x", "a");
     testNode.setProperty("x", "b");
     testNode.setProperty("x", (Value)null);
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges();
     
     assertEquals(1, logs.size());
     
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(0, states.size());
   }
   
   /**
    * Use case #4 :
    *    setProperty("x", "a");
    *    save()
    *    setProperty("x", "b");
    *    setProperty("x", null);
    * Expected persistent changes :
    *    setProperty("x", null); DELETED
    */
   public void testAddSaveAddDel() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     testNode.setProperty("x", "a");
     session.save();
     
     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);
     
     testNode.setProperty("x", "b");
     testNode.setProperty("x", (Value)null);
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges();
     
     assertEquals(1, logs.size());
          
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(1, states.size());
     
     assertEquals(ItemState.DELETED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
   }
   
   /**
    * Use case #5 :
    *    setProperty("x", "a");
    *    setProperty("x", null);
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", "c"); ADDED
    */
   public void testAddDelAdd() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session); 
     
     testNode.setProperty("x", "a");
     testNode.setProperty("x", (Value)null);
     PropertyImpl p = (PropertyImpl)testNode.setProperty("x", "c");
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges();
     
     assertEquals(1, logs.size());
          
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(1, states.size());

     assertEquals(ItemState.ADDED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
     
     assertEquals(p.getInternalIdentifier(), states.get(0).getData().getIdentifier());
   }
   
   /**
    * Use case #6 :
    *    setProperty("x", "a");
    *    save();
    *    setProperty("x", null);
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", null); DELETED
    *    setProperty("x", "c");  ADDED
    */
   public void testAddSaveDelAdd() throws Exception
   {
     Node testNode = session.getRootNode().addNode("test");
     session.save();
     
     testNode.setProperty("x", "a");
     session.save();

     TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);
     
     testNode.setProperty("x", (Value)null);
     testNode.setProperty("x", "c");
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges();
     
     assertEquals(1, logs.size());
    
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(2, states.size());
     
     assertEquals(ItemState.DELETED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
     
     assertEquals(ItemState.ADDED, states.get(1).getState());
     assertTrue(states.get(1).isPersisted());
     
     assertFalse(states.get(0).getData().getIdentifier().equals(states.get(1).getData().getIdentifier()));
   }
}
