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
package org.exoplatform.services.jcr.impl.proccess;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkerThread.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class WorkerThread
   extends Thread
{

   protected boolean stopped = false;

   protected long timeout;

   public WorkerThread(String name, long timeout)
   {
      super(name);
      this.timeout = timeout;
   }

   public WorkerThread(long timeout)
   {
      super();
      this.timeout = timeout;
   }

   @Override
   public void run()
   {
      while (!stopped)
      {
         try
         {
            callPeriodically();
            sleep(timeout);
         }
         catch (Exception e)
         {
            if (!(e instanceof InterruptedException))
               e.printStackTrace();

         }
      }
   }

   public void halt()
   {
      stopped = true;
   }

   protected abstract void callPeriodically() throws Exception;

}
