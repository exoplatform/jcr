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

package org.exoplatform.services.jcr.load.blob.thread;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 24.10.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: UserThread.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public abstract class UserThread extends Thread
{

   protected final Session threadSession;

   protected final Log threadLog;

   protected boolean process = false;

   public UserThread(Session threadSession)
   {
      super();
      int inx = getName().indexOf("-");
      setName(getClass().getSimpleName() + "-" + getName().substring(inx >= 0 ? inx + 1 : 0));
      threadLog = ExoLogger.getLogger("exo.jcr.component.core." + getName());
      this.threadSession = threadSession;
      process = true;
   }

   public void testStop()
   {
      process = false;
   }

   public void run()
   {
      testAction();
   }

   public abstract void testAction();

   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         threadSession.logout();
      }
      catch (Throwable e)
      {
      }
      super.finalize();
   }

}
