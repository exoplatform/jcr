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
package org.exoplatform.services.jcr.api.observation;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

public class TestSessionsObservation extends JcrAPIBaseTest
{

   private Node testRootWs = null;

   private Node nodeWs = null;

   private SessionImpl sessionWs1 = null;

   private Node testRootWs1 = null;

   private Node nodeWs1 = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRootWs = root.addNode("observationTest");
      nodeWs = testRootWs.addNode("node");
      session.save();

      sessionWs1 = (SessionImpl)repository.login(credentials, "ws1");
      testRootWs1 = sessionWs1.getRootNode().addNode("observationTest");
      nodeWs1 = testRootWs1.addNode("node");
      sessionWs1.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRootWs.remove();
      session.save();

      sessionWs1 = (SessionImpl)repository.login(credentials, "ws1");
      testRootWs1 = sessionWs1.getRootNode().getNode("observationTest");

      testRootWs1.remove();
      sessionWs1.save();

      super.tearDown();
   }

   public void testSessionOpen() throws RepositoryException
   {

      int counter = 0;

      SimpleListener listener = new SimpleListener("testSessionOpen", log, counter);

      try
      {
         sessionWs1.getWorkspace().getObservationManager().addEventListener(listener, Event.NODE_ADDED,
            testRootWs1.getPath() + "/n1", false, null, null, false);

         Node n1 = testRootWs1.addNode("n1"); // /observationTest/n1
         Node n1n2 = n1.addNode("n2"); // /observationTest/n1/n2
         sessionWs1.save();

         assertEquals("A events count expected 1. ", 1, listener.getCounter());
      }
      finally
      {
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(listener);
      }
   }

   public void testSessionClosed() throws Exception
   {

      int counter = 0;

      SimpleListener listener = new SimpleListener("testSessionOpen", log, counter);

      try
      {
         sessionWs1.getWorkspace().getObservationManager().addEventListener(listener, Event.NODE_ADDED,
            testRootWs1.getPath() + "/n1", false, null, null, false);

         sessionWs1.logout();

         System.gc();
         Thread.sleep(1000);

         SessionImpl anotherWs1 = (SessionImpl)repository.login(credentials, "ws1");

         try
         {
            Node n1 = anotherWs1.getRootNode().getNode(testRootWs1.getName()).addNode("n1"); // /
            // observationTest
            // /n1
            Node n1n2 = n1.addNode("n2"); // /observationTest/n1/n2
            anotherWs1.save();

            assertEquals("A events count expected 1. ", 1, listener.getCounter());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail("There are no error should be, but found " + e.getMessage());
         }
      }
      finally
      {
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(listener);
      }
   }

   public void testMultipleSessionClosed() throws Exception
   {

      SimpleListener addListener = new SimpleListener("testMultipleSessionClosed__add_nt:file", log, 0);
      SimpleListener removeListener = new SimpleListener("testMultipleSessionClosed__remove", log, 0);

      try
      {
         sessionWs1.getWorkspace().getObservationManager().addEventListener(addListener, Event.NODE_ADDED,
            testRootWs1.getPath(), true, null, new String[]{"nt:file"}, false);

         sessionWs1.logout();

         System.gc();

         Thread.sleep(1000);

         // each session will add two nodes, one of them is mix:referenceable
         int sessionCount = 300; // can't be smaller removeCount, see below
         int removeCount = 100;
         int removeIndex = 0;
         for (int i = 0; i < sessionCount; i++)
         {
            SessionImpl anotherWs1 = (SessionImpl)repository.login(credentials, "ws1");

            try
            {

               Node n1 = anotherWs1.getRootNode().getNode(testRootWs1.getName()).addNode("n" + i, "nt:file"); // /
               // observationTest
               // /
               // n1
               Node n1n2 = n1.addNode("jcr:content", "nt:unstructured"); // /observationTest/nXXXX/jcr:
               // content
               n1n2.addMixin("mix:referenceable");
               anotherWs1.save();

               if (i == removeCount)
               {
                  anotherWs1.getWorkspace().getObservationManager().addEventListener(removeListener,
                     Event.PROPERTY_REMOVED, testRootWs1.getPath(), true, null, null, true); // no local
                  removeIndex = i + removeCount;
               }
               else if (i > removeCount && i <= removeIndex)
               {
                  // remove prop:
                  // jcr:content --> jcr:primaryType, jcr:mixinTypes, jcr:uuid
                  // nt:file --> jcr:primaryType, jcr:created
                  n1.remove();
                  anotherWs1.save();
               }
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail("There are no error should be, but found " + e.getMessage());
            }
            finally
            {
               anotherWs1.logout();
            }
         }

         assertEquals("A events count expected ", sessionCount, addListener.getCounter());
         assertEquals("A events count expected ", removeCount * 5, removeListener.getCounter());

      }
      finally
      {
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(addListener);
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(removeListener);
      }
   }

   public void testListenerRemoved() throws Exception
   {

      SimpleListener listener1 = new SimpleListener("testListenerRemoved1", log, 0);
      SimpleListener listener2 = new SimpleListener("testListenerRemoved2", log, 0);

      try
      {
         sessionWs1.getWorkspace().getObservationManager().addEventListener(listener1, Event.NODE_ADDED,
            testRootWs1.getPath() + "/n1", true, null, null, false);

         sessionWs1.getWorkspace().getObservationManager().addEventListener(listener2, Event.PROPERTY_CHANGED,
            testRootWs1.getPath() + "/n1", true, null, null, false);

         sessionWs1.logout();

         SessionImpl anotherWs1 = (SessionImpl)repository.login(credentials, "ws1");

         try
         {
            Node n1 = anotherWs1.getRootNode().getNode(testRootWs1.getName()).addNode("n1"); // /
            // observationTest
            // /n1
            Node n1n2 = n1.addNode("n2"); // /observationTest/n1/n2
            n1n2.setProperty("prop", "observation property");
            anotherWs1.save();

            n1n2.setProperty("prop", "property edition 1"); // change 1
            anotherWs1.save();

            anotherWs1.getWorkspace().getObservationManager().removeEventListener(listener1);

            Node n1n3 = n1.addNode("n3"); // /observationTest/n1/n3
            n1n3.setProperty("prop", "observation property");

            n1n2.setProperty("prop", "property edition 2"); // change 2

            anotherWs1.save();

            assertEquals("A events count expected", 1, listener1.getCounter());
            assertEquals("A events count expected", 2, listener2.getCounter());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail("There are no error should be, but found " + e.getMessage());
         }
      }
      finally
      {
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(listener1);
         sessionWs1.getWorkspace().getObservationManager().removeEventListener(listener2);
      }
   }

   public void testMoveOnClosedSession() throws Exception
   {

      testRootWs1.addNode("newNode");
      sessionWs1.save();

      int counter = 0;

      SimpleListener listener = new SimpleListener("testSessionOpen", log, counter);

      Session sessionWs1ForListener = repository.login(credentials, "ws1");

      sessionWs1ForListener.getWorkspace().getObservationManager().addEventListener(listener,
         Event.NODE_ADDED | Event.NODE_REMOVED, testRootWs1.getPath() + "/", false, null, null, false);

      sessionWs1ForListener.logout();

      sessionWs1.logout();
      sessionWs1.getWorkspace().move(testRootWs1.getPath() + "/newNode", testRootWs1.getPath() + "/newNode2");
      assertEquals(2, listener.getCounter());
   }
}
