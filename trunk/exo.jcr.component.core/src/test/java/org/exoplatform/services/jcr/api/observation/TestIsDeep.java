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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListenerIterator;

/**
 * Created by The eXo Platform SAS 10.05.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestIsDeep.java 12336 2008-03-25 12:54:02Z ksm $
 */
public class TestIsDeep extends JcrAPIBaseTest
{

   private Node testObservation;

   public void setUp() throws Exception
   {
      super.setUp();

      testObservation = root.addNode("testObservation");
      root.save();
   }

   public void tearDown() throws Exception
   {
      EventListenerIterator listeners = this.workspace.getObservationManager().getRegisteredEventListeners();
      while (listeners.hasNext())
      {
         this.workspace.getObservationManager().removeEventListener(listeners.nextEventListener());
      }
      testObservation.remove();
      root.save();

      super.tearDown();
   }

   public void testIsDeepFalseNodeAdd() throws RepositoryException
   {
      Integer counter = 0;
      SimpleListener listener = new SimpleListener("IsDeepFalseNodeAdd", log, counter);

      workspace.getObservationManager().addEventListener(listener, Event.NODE_ADDED, testObservation.getPath() + "/n1",
         false, null, null, false);

      Node n1 = testObservation.addNode("n1"); // /testObservation/n1
      Node n1n2 = n1.addNode("n2"); // /testObservation/n1/n2
      testObservation.save();

      assertTrue("A events count expected 1. Was: " + listener.getCounter(), listener.getCounter() == 1);

      checkItemsExisted(new String[]{n1.getPath(), n1n2.getPath()}, null);
   }

   public void testIsDeepTrueNodeAdd() throws RepositoryException
   {
      Integer counter = 0;
      SimpleListener listener = new SimpleListener("IsDeepTrueNodeAdd", log, counter);

      workspace.getObservationManager().addEventListener(listener, Event.NODE_ADDED, testObservation.getPath() + "/n1",
         true, null, null, false);

      Node n1 = testObservation.addNode("n1"); // /testObservation/n1
      Node n1n2 = n1.addNode("n2"); // /testObservation/n1/n2
      testObservation.save();

      assertTrue("A events count expected 1. Was: " + listener.getCounter(), listener.getCounter() == 1);

      checkItemsExisted(new String[]{n1.getPath(), n1n2.getPath()}, null);
   }

   public void testIsDeepFalseNodeRemove() throws RepositoryException
   {
      Integer counter = 0;
      SimpleListener listener = new SimpleListener("IsDeepFalseNodeRemove", log, counter);

      workspace.getObservationManager().addEventListener(listener, Event.NODE_REMOVED,
         testObservation.getPath() + "/n1", false, null, null, false);

      Node n1 = testObservation.addNode("n1"); // /testObservation/n1
      Node n1n2 = n1.addNode("n2"); // /testObservation/n1/n2
      testObservation.save();

      n1n2.remove();
      testObservation.save();
      assertTrue("A events count expected 1. Was: " + listener.getCounter(), listener.getCounter() == 1);
      checkItemsExisted(new String[]{n1.getPath()}, new String[]{n1n2.getPath()});

      n1.remove();
      testObservation.save();
      assertTrue("A events count expected 1. Was: " + listener.getCounter(), listener.getCounter() == 1);
      checkItemsExisted(null, new String[]{n1.getPath(), n1n2.getPath()});
   }

   public void testIsDeepTrueNodeRemove() throws RepositoryException
   {
      Integer counter = 0;
      SimpleListener listener = new SimpleListener("IsDeepTrueNodeRemove", log, counter);

      workspace.getObservationManager().addEventListener(listener, Event.NODE_REMOVED,
         testObservation.getPath() + "/n1", true, null, null, false);

      Node n1 = testObservation.addNode("n1"); // /testObservation/n1
      Node n1n2 = n1.addNode("n2"); // /testObservation/n1/n2
      testObservation.save();

      n1n2.remove();
      testObservation.save();
      assertTrue("A events count expected 1. Was: " + listener.getCounter(), listener.getCounter() == 1);
      checkItemsExisted(new String[]{n1.getPath()}, new String[]{n1n2.getPath()});

      n1.remove();
      testObservation.save();
      assertTrue("A events count expected 2. Was: " + listener.getCounter(), listener.getCounter() == 1);
      checkItemsExisted(null, new String[]{n1.getPath(), n1n2.getPath()});
   }
}
