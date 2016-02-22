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
package org.exoplatform.services.jcr.load;

import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 15.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSameUserLogin.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestSameUserLogin extends BaseUsecasesTest
{

   private final Log TLOG = ExoLogger.getLogger("exo.jcr.component.core.TestSameUserLogin");

   class LoginThread extends Thread
   {

      private final Credentials user;

      private final Object lock;

      private Session session;

      LoginThread(int number, Credentials user, Object runLock)
      {
         super("LoginThread-" + number);
         this.user = user;
         this.lock = runLock;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {

         try
         {
            synchronized (lock)
            {
               lock.wait();
            }
            session = repository.login(user, "ws1");
            TLOG.info("Login ok " + System.currentTimeMillis());

            session.logout();
            TLOG.info("Logout ok " + System.currentTimeMillis());
         }
         catch (LoginException e)
         {
            e.printStackTrace();
         }
         catch (NoSuchWorkspaceException e)
         {
            e.printStackTrace();
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
         }
         catch (InterruptedException e)
         {
            e.printStackTrace();
         }
      }

      void done()
      {
         if (session != null)
            session.logout();
      }
   }

   public void testMultiThreadLogin()
   {

      LoginThread[] queue = new LoginThread[20];

      Object runLock = new Object();

      for (int i = 0; i < queue.length; i++)
      {
         queue[i] = new LoginThread(i, new CredentialsImpl("root", "exo".toCharArray()), runLock);
         queue[i].start();
      }

      // try start all together
      synchronized (runLock)
      {
         runLock.notifyAll();
      }

      for (LoginThread lt : queue)
      {
         lt.done();
      }
   }

   /**
    * LoginThread modified by Tomasz Wysocki according to http://jira.exoplatform.org/browse/JCR-875
    */
   class LoginThread2 extends Thread
   {

      private final Credentials user;

      private volatile boolean stop = false;

      private Session session;

      private int pass;

      private int passes;

      private final int number;

      LoginThread2(int number, Credentials user, Object runLock, int passes)
      {
         super("LoginThread-" + number);
         this.number = number;
         this.user = user;
         this.passes = passes;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {

         try
         {
            TLOG.info("#" + number + " Starting Login/Logout " + passes + " cycles.");
            while (!stop && pass < passes)
            {
               try
               {
                  session = repository.login(user, "ws1");
                  pass++;
               }
               finally
               {
                  session.logout();
               }
            }
            TLOG.info("#" + number + " Done Login/Logout " + pass + "/" + passes + " cycles.");
         }
         catch (LoginException e)
         {
            e.printStackTrace();
         }
         catch (NoSuchWorkspaceException e)
         {
            e.printStackTrace();
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
         }
      }

      int done() throws InterruptedException
      {
         this.stop = true;
         join();
         TLOG.info("#" + number + " Login/Logout cycles completed :" + pass);
         return pass;
      }
   }

   /**
    * Test modified by Tomasz Wysocki according to http://jira.exoplatform.org/browse/JCR-875
    */
   public void testMultiThreadLogin2() throws InterruptedException
   {

      LoginThread2[] queue = new LoginThread2[20];

      Object runLock = new Object();

      int passes = 10000;

      for (int i = 0; i < queue.length; i++)
      {
         queue[i] = new LoginThread2(i, new CredentialsImpl("root", "exo".toCharArray()), runLock, passes);
      }

      for (int i = 0; i < queue.length; i++)
      {
         queue[i].start();
      }

      Thread.sleep(30000);

      for (LoginThread2 lt : queue)
      {
         assertEquals(passes, lt.done());
      }
   }

}
