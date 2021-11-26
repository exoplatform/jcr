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
