package org.exoplatform.services.jcr.impl.dataflow;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TesterItemsPersistenceListener;

public class TestOptimizationChangesLog extends JcrImplBaseTest
{
   /**
    * Use case :
    *    setProperty("x", "a");
    *    setProperty("x", "b");
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", "a"); ADDED
    *    setProperty("x", "c"); UPDATED
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
     assertEquals(3, states.size());

     assertEquals(ItemState.ADDED, states.get(0).getState());
     assertTrue(states.get(0).isPersisted());
     
     assertEquals(ItemState.UPDATED, states.get(1).getState());
     assertFalse(states.get(1).isPersisted());
     
     assertEquals(ItemState.UPDATED, states.get(2).getState());
     assertTrue(states.get(2).isPersisted());
   }
   
   /**
    * Use case :
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
     assertEquals(2, states.size());

     assertEquals(ItemState.UPDATED, states.get(0).getState());
     assertFalse(states.get(0).isPersisted());
     
     assertEquals(ItemState.UPDATED, states.get(1).getState());
     assertTrue(states.get(1).isPersisted());
   }
   
   /**
    * Use case :
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
     assertEquals(3, states.size());

     assertEquals(ItemState.ADDED, states.get(0).getState());
     assertFalse(states.get(0).isPersisted());
     
     assertEquals(ItemState.UPDATED, states.get(1).getState());
     assertFalse(states.get(1).isPersisted());
     
     assertEquals(ItemState.DELETED, states.get(2).getState());
     assertFalse(states.get(2).isPersisted());
   }
   
   /**
    * Use case :
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
     assertEquals(2, states.size());
     
     assertEquals(ItemState.UPDATED, states.get(0).getState());
     assertFalse(states.get(0).isPersisted());
     
     assertEquals(ItemState.DELETED, states.get(1).getState());
     assertTrue(states.get(1).isPersisted());
   }
   
   /**
    * Use case :
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
     testNode.setProperty("x", "c");
     session.save();
     
     List<TransactionChangesLog> logs = pListener.pushChanges();
     
     assertEquals(1, logs.size());
          
     List<ItemState> states = logs.get(0).getAllStates();
     assertEquals(3, states.size());

     assertEquals(ItemState.ADDED, states.get(0).getState());
     assertFalse(states.get(0).isPersisted());
     
     assertEquals(ItemState.DELETED, states.get(1).getState());
     assertFalse(states.get(1).isPersisted());
     
     assertEquals(ItemState.ADDED, states.get(2).getState());
     assertTrue(states.get(2).isPersisted());
     
     assertFalse(states.get(0).getData().getIdentifier().equals(states.get(2).getData().getIdentifier()));
   }
   
   /**
    * Use case :
    *    setProperty("x", "a");
    *    save();
    *    setProperty("x", null);
    *    setProperty("x", "c");
    * Expected persistent changes :
    *    setProperty("x", null); DELETED
    *    setProperty("x", "c"); ADDED
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
