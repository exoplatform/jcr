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
import org.exoplatform.services.log.Log;

import java.util.Calendar;

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

   private EventListener listener;

   private EventListener listener1;

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
      EventListener listener = new DummyListener(this.log);
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
      EventListener listener = new DummyListener(this.log);

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

   public void testPropertyEventGeneration() throws RepositoryException
   {
      ObservationManager observationManager = this.workspace.getObservationManager();
      EventListener listener = new DummyListener(this.log);

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
    * SimpleListener(this.log); observationManager.addEventListener(listener,
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
      EventListener listener = new DummyListener(this.log);
      EventListener listener1 = new DummyListener1(this.log);

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

      EventListener listener = new DummyListener(this.log);
      observationManager.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
         | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/", true, null, null, false);

      session.getWorkspace().clone("ws2", "/testCloneEvents", "/testRoot/testCloneEvents", true);

      checkEventNumAndCleanCounter(4);

      observationManager.removeEventListener(listener);

   }

   private void checkEventNumAndCleanCounter(int cnt)
   {
      assertEquals(cnt, counter);
      counter = 0;
   }

   private static class DummyListener implements EventListener
   {
      private Log log;

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

}
