/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.api.observation;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.Log;

import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestObservationManager.java 15053 2008-06-02 10:31:38Z
 *          andrew00x $
 */
public class TestObservationManager extends JcrAPIBaseTest
{

   private static int counter;

   private Node testRoot;

   public void setUp() throws Exception
   {
      super.setUp();
      counter = 0;
      testRoot = root.addNode("testRoot");
      root.save();
   }

   public void tearDown() throws Exception
   {
      EventListenerIterator listeners = this.workspace.getObservationManager().getRegisteredEventListeners();
      while (listeners.hasNext())
      {
         this.workspace.getObservationManager().removeEventListener(listeners.nextEventListener());
      }
      testRoot.remove();
      root.save();
      // testRoot.save();ipossible to call save() on removed node

      super.tearDown();
   }

   public void testObtainObservationManager() throws RepositoryException
   {
      ObservationManager om = this.workspace.getObservationManager();
      assertNotNull(om);

      // Not the same as in ws
      Session session2 = repository.login(credentials, "ws2");
      ObservationManager om2 = session2.getWorkspace().getObservationManager();
      assertNotNull(om2);
      assertFalse(om.equals(om2));
   }

   public void testListenerRegistration() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();
      assertEquals(0, observationManager.getRegisteredEventListeners().getSize());
      EventListener listener = new DummyListener(log);
      observationManager.addEventListener(listener, Event.PROPERTY_ADDED | Event.NODE_ADDED, "/", true,
         new String[]{"0"}, new String[]{"nt:base"}, false);
      assertEquals(1, observationManager.getRegisteredEventListeners().getSize());

      // [PN] 16.06.07
      // Listener in observation manager is per session, global listeners is
      // registered in observation
      // registry.
      // Listeners list can be acquired there (Impl level).
      // Session session1 = repository.login(credentials,
      // this.workspace.getName()); // the same ws
      // ObservationManager observationManager1 =
      // session1.getWorkspace().getObservationManager();
      // assertEquals(1,
      // observationManager1.getRegisteredEventListeners().getSize());

      Session session2 = repository.login(credentials, "ws2"); // another ws
      ObservationManager observationManager2 = session2.getWorkspace().getObservationManager();
      assertEquals(0, observationManager2.getRegisteredEventListeners().getSize());

      observationManager.removeEventListener(listener);
      assertEquals(0, observationManager.getRegisteredEventListeners().getSize());
   }

   public void testNodeEventGeneration() throws RepositoryException
   {
      // ObservationManager observationManager =
      // this.workspace.getObservationManager();
      ObservationManager observationManager = repository.getSystemSession("ws").getWorkspace().getObservationManager();
      EventListener listener = new DummyListener(log);

      // Add/remove node by explicit path
      observationManager.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED, "/childNode", false, null,
         null, false);
      testRoot.addNode("childNode", "nt:unstructured");
      testRoot.addNode("childNode1", "nt:unstructured");
      root.save();
      checkEventNumAndCleanCounter(0);
      testRoot.getNode("childNode").remove();
      testRoot.getNode("childNode1").remove();
      testRoot.save();
      checkEventNumAndCleanCounter(0);
      observationManager.removeEventListener(listener);

      // Add node by descendant path
      observationManager.addEventListener(listener, Event.NODE_ADDED, "/", true, null, null, false);
      testRoot.addNode("childNode", "nt:unstructured");
      testRoot.addNode("childNode1", "nt:unstructured");
      testRoot.save();
      checkEventNumAndCleanCounter(2);
      testRoot.getNode("childNode").remove();
      testRoot.getNode("childNode1").remove();
      testRoot.save();
      checkEventNumAndCleanCounter(0); // no remove event
      observationManager.removeEventListener(listener);

      // Add node by node type
      observationManager.addEventListener(listener, Event.NODE_ADDED, "/", true, null, new String[]{"nt:unstructured"},
         false);
      Node cn = testRoot.addNode("childNode", "nt:folder");
      // associated parent is not 'nt:unstructured' - no event will be generated
      cn.addNode("childNode1", "nt:hierarchyNode");
      testRoot.save();
      checkEventNumAndCleanCounter(1);
      observationManager.removeEventListener(listener);

      // Add node by UUID (never knows the UUID before adding :) )
      observationManager.addEventListener(listener, Event.NODE_ADDED, "/", true, new String[]{"0"},
         new String[]{"nt:unstructured"}, false);
      testRoot.addNode("childNode", "nt:unstructured");
      testRoot.save();
      checkEventNumAndCleanCounter(0);
      observationManager.removeEventListener(listener);
   }

   public void testRemoveNodeEvents() throws Exception
   {
      session.getWorkspace().getObservationManager().addEventListener(new DummyListener(log),
         Event.NODE_ADDED | Event.NODE_REMOVED, "/", true, null, new String[]{"nt:file"}, false);

      Node file = testRoot.addNode("test", "nt:file");
      Node d = file.addNode("jcr:content", "nt:resource");
      d.setProperty("jcr:mimeType", "text/plain");
      d.setProperty("jcr:lastModified", Calendar.getInstance());
      d.setProperty("jcr:data", "test content");
      session.save();

      checkEventNumAndCleanCounter(1);

      file.remove();
      session.save();
      checkEventNumAndCleanCounter(1);
   }

   public void testMoveNodeEvents() throws Exception
   {
      DummyListener listener = new DummyListener(log);
      session.getWorkspace().getObservationManager().addEventListener(listener,
              ExtendedEvent.NODE_MOVED, "/", true, null, null, false);

      Node n1 = testRoot.addNode("n1");
      Node n2 = n1.addNode("n2");
      Node n3 = testRoot.addNode("n3");
      Node n4 = testRoot.addNode("n4");
      n4.addNode("n5");
      session.save();

      session.move(n4.getPath(), n2.getPath());
      session.save();
      checkEventNumAndCleanCounter(1);
      assertEquals("/testRoot/n4", listener.getInfo().get(ExtendedEvent.SRC_ABS_PATH));
      assertEquals("/testRoot/n1/n2[2]", listener.getInfo().get(ExtendedEvent.DEST_ABS_PATH));

      session.move(n1.getPath(), n3.getPath()+"/n6");
      session.save();
      checkEventNumAndCleanCounter(1);
      assertEquals("/testRoot/n1", listener.getInfo().get(ExtendedEvent.SRC_ABS_PATH));
      assertEquals("/testRoot/n3/n6", listener.getInfo().get(ExtendedEvent.DEST_ABS_PATH));

      session.getWorkspace().move("/testRoot/n3/n6","/testRoot/n7");
      checkEventNumAndCleanCounter(1);
      assertEquals("/testRoot/n3/n6", listener.getInfo().get(ExtendedEvent.SRC_ABS_PATH));
      assertEquals("/testRoot/n7", listener.getInfo().get(ExtendedEvent.DEST_ABS_PATH));

      session.getWorkspace().move("/testRoot/n3","/testRoot/n7");
      checkEventNumAndCleanCounter(1);
      assertEquals("/testRoot/n3", listener.getInfo().get(ExtendedEvent.SRC_ABS_PATH));
      assertEquals("/testRoot/n7[2]", listener.getInfo().get(ExtendedEvent.DEST_ABS_PATH));
   }

   public void testOrderNodeEvents() throws Exception
   {
      DummyListener listener = new DummyListener(log);
      session.getWorkspace().getObservationManager().addEventListener(listener,
                ExtendedEvent.NODE_MOVED, "/", true, null, null, false);

      testRoot.addNode("n1");
      testRoot.addNode("n2");
      testRoot.addNode("n1");
      testRoot.addNode("n3");
      testRoot.addNode("n2");
      testRoot.addNode("n4");
      testRoot.save();
       
      testRoot.orderBefore("n1[2]","n1");
      session.save();
      checkEventNumAndCleanCounter(1);
      assertEquals("/testRoot/n1[2]", listener.getInfo().get(ExtendedEvent.SRC_CHILD_REL_PATH));
      assertEquals("/testRoot/n1", listener.getInfo().get(ExtendedEvent.DEST_CHILD_REL_PATH));

      testRoot.orderBefore("n2","n3");
      session.save();
      checkEventNumAndCleanCounter(0);
   }

   public void testPropertyEventGeneration() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();
      EventListener listener = new DummyListener(log);

      if (log.isDebugEnabled())
         log.debug("SET PROP>>");
      // Add/remove node by explicit path
      observationManager.addEventListener(listener, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
         | Event.PROPERTY_REMOVED, "/", true, null, null, false);
      Node node = testRoot.addNode("childNode", "nt:unstructured");
      Property prop = node.setProperty("prop", "prop");
      root.save();
      if (log.isDebugEnabled())
         log.debug("SET PROP>>");

      // SET /childNode/jcr:primaryType and /childNode/prop
      checkEventNumAndCleanCounter(2);
      prop.setValue("test1");
      testRoot.save();
      checkEventNumAndCleanCounter(1);
      prop.remove();
      testRoot.save();
      checkEventNumAndCleanCounter(1);
      observationManager.removeEventListener(listener);
   }

   /*
    * public void testMultiEventGeneration() throws RepositoryException {
    * ObservationManager observationManager =
    * this.workspace.getObservationManager(); EventListener listener = new
    * SimpleListener(log); observationManager.addEventListener(listener,
    * Event.NODE_ADDED|Event.PROPERTY_ADDED
    * |Event.PROPERTY_REMOVED|Event.NODE_REMOVED|Event.PROPERTY_CHANGED, "/",
    * true, null, null, false); Node node = root.addNode("childNode",
    * "nt:unstructured"); root.save(); Property prop = node.setProperty("prop",
    * "test"); root.save(); checkAndCleanCounter(3); prop.setValue("test1");
    * root.save(); checkAndCleanCounter(1); prop.remove(); root.save();
    * checkAndCleanCounter(1); node.remove(); root.save();
    * checkAndCleanCounter(1); observationManager.removeEventListener(listener);
    * }
    */

   public void testMultiListener() throws RepositoryException
   {

      ObservationManager observationManager = this.workspace.getObservationManager();
      EventListener listener = new DummyListener(log);
      EventListener listener1 = new DummyListener1(log);

      observationManager.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
         | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/testRoot", true, null, null, false);
      observationManager.addEventListener(listener1, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
         | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/testRoot", false, null, null, false);

      Node node = testRoot.addNode("childNode", "nt:unstructured");
      root.save();
      Property prop = testRoot.setProperty("prop", "test");
      root.save();
      checkEventNumAndCleanCounter(5); // 3 by Dy listener + 2 by listener1

      observationManager.removeEventListener(listener);
      observationManager.removeEventListener(listener1);

   }

   public void testCloneEvents() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();

      Session session2 = repository.login(credentials, "ws2");
      Node root = session2.getRootNode();
      Node node = root.addNode("testCloneEvents");
      node.addMixin("mix:referenceable");
      session2.save();

      // Node node = testRoot.addNode("testCloneEvents", "nt:unstructured");
      session.save();

      checkEventNumAndCleanCounter(0);

      EventListener listener = new DummyListener(log);
      observationManager.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
         | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/", true, null, null, false);

      session.getWorkspace().clone("ws2", "/testCloneEvents", "/testRoot/testCloneEvents", true);

      checkEventNumAndCleanCounter(4);

      observationManager.removeEventListener(listener);

   }

   public void testRemoveSourceNodeEvents() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();
      testRoot.addNode("testRemoveSourceNode");
      EventListener listener = new RemoveDummyListener(log, this.repository, this.credentials);
      observationManager.addEventListener(listener, Event.NODE_ADDED, "/", true, null, null, false);
      root.save();
      Session session2 = repository.login(credentials, "ws2");
      Session session = repository.login(credentials, "ws");
      assertFalse(session.itemExists("/testRoot/testRemoveSourceNode"));
      assertTrue(session2.itemExists("/testRemoveSourceNode"));
      observationManager.removeEventListener(listener);
   }

   public void testWithAnUnknownNodeType() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();
      checkEventNumAndCleanCounter(0);

      EventListener listener = new DummyListener(log);
      try
      {
         observationManager.addEventListener(listener, Event.NODE_ADDED, "/", true, null, new String[]{"nt:anUnknownNodeType"}, false);
         testRoot.addNode("testWithAnUnknownNodeType");
         root.save();
         checkEventNumAndCleanCounter(0);
      }
      finally
      {
         observationManager.removeEventListener(listener);
      }
   }

   private void checkEventNumAndCleanCounter(int cnt)
   {
      assertEquals(cnt, counter);
      counter = 0;
   }

   private static class DummyListener implements EventListener
   {
      private Log log;

      private Map<String, String> info ;

      public DummyListener(Log log)
      {
         this.log = log;
      }

      public void onEvent(EventIterator events)
      {
         while (events.hasNext())
         {
            Event event = events.nextEvent();
            counter++;
            if(event.getType()== ExtendedEvent.NODE_MOVED)
            {
                try
                {
                    info=((ExtendedEvent)event).getInfo();
                }
                catch (RepositoryException e)
                {
                   log.error(e.getMessage(),e);
                }
            }
            try
            {
               if (log.isDebugEnabled())
                  log.debug("EVENT fired by SimpleListener " + event.getPath() + " " + event.getType());
            }
            catch (RepositoryException e)
            {
               e.printStackTrace();
            }
         }
      }
      public Map<String, String> getInfo()
      {
         return info;
      }
   }

   private static class DummyListener1 implements EventListener
   {
      private Log log;

      public DummyListener1(Log log)
      {
         this.log = log;
      }

      public void onEvent(EventIterator events)
      {
         while (events.hasNext())
         {
            Event event = events.nextEvent();
            counter++;
            if (log.isDebugEnabled())
               log.debug("EVENT fired by SimpleListener-1 " + event + " " + event.getType());
         }
      }
   }

   private static class RemoveDummyListener implements EventListener
   {
      protected Log log;

      protected RepositoryImpl repository;

      protected CredentialsImpl credentials;

      public RemoveDummyListener(Log log, RepositoryImpl repository, CredentialsImpl credentials)
      {
         this.log = log;
         this.repository = repository;
         this.credentials = credentials;
      }

      public void onEvent(EventIterator events)
      {
         while (events.hasNext())
         {
            Event event = events.nextEvent();
            counter++;
            try
            {
               String path = event.getPath();
               Session session2 = repository.login(credentials, "ws2");
               session2.getWorkspace().clone("ws", path, "/testRemoveSourceNode", true);
               Session session = repository.login(credentials, "ws");
               session.getItem(path).remove();
               session.save();
            }
            catch (RepositoryException re)
            {
               System.out.println(re.getMessage());
               if (log.isErrorEnabled())
               {
                  log.error(re.getMessage());
               }
            }
            if (log.isDebugEnabled())
               log.debug("EVENT fired by RemoveDummyListener " + event + " " + event.getType());
         }
      }
   }

}
