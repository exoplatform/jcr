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
package org.exoplatform.services.ftp;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.StandaloneContainer;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class FtpServerTest extends BaseFtpTest
{

   private static boolean eventOccurs = false;

   public void testSetContainer() throws Exception
   {
      workspace.getObservationManager().addEventListener(new DummyEventListener(),
         Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/ftp-root", true, null, null, false);
      String filename = "test0.txt";
      String fileContent = "exo ftp server test\n";
      try
      {
         connect();
         pwd();
         cwd("ws");
         cwd("ftp-root");
         pwd();
         stor(fileContent.getBytes(), filename);
         String retrieved = new String(retr(filename));
         assertEquals(fileContent, retrieved);
      }
      finally
      {
         disconnect();
      }
      assertTrue(eventOccurs);
   }

   public static class DummyEventListener implements EventListener
   {

      public void onEvent(EventIterator event)
      {
         eventOccurs = true;
         assertNotNull(ExoContainerContext.getCurrentContainer());
         assertTrue(ExoContainerContext.getCurrentContainer() instanceof StandaloneContainer);
      }
   }

}
