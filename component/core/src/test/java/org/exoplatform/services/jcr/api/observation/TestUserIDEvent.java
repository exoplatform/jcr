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
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak
 * alex.reshetnyak@exoplatform.org.ua reshetnyak.alex@gmail.com 20.06.2007
 * 10:36:07
 * 
 * @version $Id: TestUserIDEvent.java 20.06.2007 10:36:07 rainfox
 */
public class TestUserIDEvent extends JcrAPIBaseTest implements EventListener
{

   @Override
   protected void tearDown() throws Exception
   {
      session.getWorkspace().getObservationManager().removeEventListener(this);

      super.tearDown();
   }

   public void testUserId() throws Exception
   {

      session.getWorkspace().getObservationManager().addEventListener(this, Event.NODE_ADDED, root.getPath(), true,
         null, null, false);

      CredentialsImpl credentialsEXO = new CredentialsImpl("exo", "exo".toCharArray());

      SessionImpl sessionEXO = (SessionImpl)repository.login(credentialsEXO, "ws");

      Node rootEXO = sessionEXO.getRootNode();

      rootEXO.addNode("addNode");

      sessionEXO.save();
   }

   public void onEvent(EventIterator events)
   {
      try
      {
         if (events.hasNext())
         {
            Event event = events.nextEvent();
            String userId = event.getUserID();

            log.debug("UserID     : " + userId);
            log.debug("Event path : " + event.getPath());

            assertEquals("exo", userId);
         }
      }
      catch (RepositoryException e)
      {
         fail("Repository exeption");
      }
   }
}
