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

import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * Created by The eXo Platform SAS 10.05.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SimpleListener.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class SimpleListener implements EventListener
{

   private Log log;

   private String name;

   private int counter;

   public SimpleListener(String name, Log log, Integer counter)
   {
      this.log = log;
      this.name = (name == null ? "SimpleListener-" + System.currentTimeMillis() : name);
      this.counter = counter;
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
               log.debug("EVENT fired by " + name + " " + event.getPath() + " " + event.getType());
         }
         catch (RepositoryException e)
         {
            log.error("Error in " + name, e);
         }
      }
   }

   public int getCounter()
   {
      return counter;
   }

   public String getName()
   {
      return name;
   }
}
