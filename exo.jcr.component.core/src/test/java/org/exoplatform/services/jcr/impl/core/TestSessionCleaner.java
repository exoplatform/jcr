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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestSessionCleaner.java 14508 2008-05-20 10:07:45Z ksm $
 */
public class TestSessionCleaner extends JcrImplBaseTest
{
   private final static int AGENT_COUNT = 10;

   private SessionRegistry sessionRegistry;

   private long oldTimeOut;

   private final static long TEST_SESSION_TIMEOUT = 20000;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      sessionRegistry = (SessionRegistry)session.getContainer().getComponentInstanceOfType(SessionRegistry.class);
      oldTimeOut = sessionRegistry.timeOut;
      sessionRegistry.timeOut = TEST_SESSION_TIMEOUT;
      sessionRegistry.stop();
      Thread.yield();
      sessionRegistry.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      sessionRegistry.stop();
      sessionRegistry.timeOut = oldTimeOut;
      Thread.yield();
      sessionRegistry.start();
   }

   public void testSessionRemove() throws LoginException, NoSuchWorkspaceException, RepositoryException,
      InterruptedException
   {
      SessionImpl session2 = (SessionImpl)repository.login(credentials, "ws");

      // Create a weak reference to the session
      WeakReference<SessionImpl> ref = new WeakReference<SessionImpl>(session2);

      assertTrue(session2.isLive());

      assertNotNull(sessionRegistry);

      Thread.sleep(SessionRegistry.DEFAULT_CLEANER_TIMEOUT + 1000);

      assertFalse(session2.isLive());

      // Dereference the session explicitely
      session2 = null;

      // Make a GC
      forceGC();

      // The weak reference must now be null
      assertNull(ref.get());

   }

   public void testSessionLoginLogoutSimultaneouslyMultiThread() throws Exception
   {
      assertNotNull(sessionRegistry);

      class AgentLogin extends Thread
      {

         Random random = new Random();

         SessionImpl workSession;

         boolean sessionStarted = false;

         public AgentLogin()
         {
         }

         @Override
         public void run()
         {
            try
            {
               Thread.sleep(SessionRegistry.DEFAULT_CLEANER_TIMEOUT - random.nextInt(200) + 200);

               workSession = (SessionImpl)repository.login(credentials, "ws");
               sessionStarted = true;

            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail("Exception should not be thrown");
            }
         }
      }

      class AgentLogout extends Thread
      {
         AgentLogin agentLogin;

         public AgentLogout(AgentLogin agentLogin)
         {
            this.agentLogin = agentLogin;
         }

         @Override
         public void run()
         {
            try
            {
               while (!agentLogin.sessionStarted)
               {
                  Thread.sleep(50);
               }

               if (agentLogin.workSession.isLive())
               {
                  agentLogin.workSession.logout();
               }

            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail("Exception should not be thrown");
            }
         }
      }

      Session workSession = (SessionImpl)repository.login(credentials, "ws");
      while (workSession.isLive())
      {
         Thread.sleep(100);
      }

      // start
      List<Object> agents = new ArrayList<Object>();

      for (int i = 0; i < AGENT_COUNT; i++)
      {
         AgentLogin agentLogin = new AgentLogin();
         agents.add(agentLogin);
         agentLogin.start();

         AgentLogout agentLogout = new AgentLogout(agentLogin);
         agents.add(agentLogout);
         agentLogout.start();
      }

      // wait to stop all threads
      boolean isNeedWait = true;
      while (isNeedWait)
      {
         isNeedWait = false;
         for (int i = 0; i < AGENT_COUNT * 2; i++)
         {
            Thread agent = (Thread)agents.get(i);
            if (agent.isAlive())
            {
               isNeedWait = true;
               break;
            }
         }
         Thread.sleep(1000);
      }

      assertFalse(sessionRegistry.isInUse("ws"));
   }

   public void testSessionLoginLogoutMultiThread() throws InterruptedException
   {
      assertNotNull(sessionRegistry);

      class AgentLogin extends Thread
      {

         SessionImpl workSession;

         int sleepTime;

         boolean sessionStarted = false;

         public AgentLogin(int sleepTime)
         {
            this.sleepTime = sleepTime;
         }

         @Override
         public void run()
         {
            try
            {
               Thread.sleep(sleepTime);
               workSession = (SessionImpl)repository.login(credentials, "ws");
               sessionStarted = true;

            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail("Exception should not be thrown");
            }
         }
      }

      class AgentLogout extends Thread
      {
         AgentLogin agentLogin;

         public AgentLogout(AgentLogin agentLogin)
         {
            this.agentLogin = agentLogin;
         }

         @Override
         public void run()
         {
            try
            {
               while (!agentLogin.sessionStarted)
               {
                  Thread.sleep(1000);
               }

               Thread.sleep(SessionRegistry.DEFAULT_CLEANER_TIMEOUT / 2);

               if (agentLogin.workSession.isLive())
               {
                  agentLogin.workSession.logout();
               }

            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail("Exception should not be thrown");
            }
         }
      }

      // start
      List<Object> agents = new ArrayList<Object>();

      int sleepTime = 0;
      for (int i = 0; i < AGENT_COUNT; i++)
      {
         AgentLogin agentLogin = new AgentLogin(sleepTime);
         agents.add(agentLogin);
         agentLogin.start();

         AgentLogout agentLogout = new AgentLogout(agentLogin);
         agents.add(agentLogout);
         agentLogout.start();

         sleepTime =
            SessionRegistry.DEFAULT_CLEANER_TIMEOUT / 10
               + (sleepTime >= 2 * SessionRegistry.DEFAULT_CLEANER_TIMEOUT ? 0 : sleepTime);
      }

      // wait to stop all threads
      boolean isNeedWait = true;
      while (isNeedWait)
      {
         isNeedWait = false;
         for (int i = 0; i < AGENT_COUNT * 2; i++)
         {
            Thread agent = (Thread)agents.get(i);
            if (agent.isAlive())
            {
               isNeedWait = true;
               break;
            }
         }
         Thread.sleep(100);
      }

      assertFalse(sessionRegistry.isInUse("ws"));
   }

   public void testSessionRemoveMultiThread() throws InterruptedException
   {
      assertNotNull(sessionRegistry);
      final Random random = new Random();
      class Agent extends Thread
      {
         boolean result = false;

         boolean active = false;

         public Agent()
         {
            active = random.nextBoolean();
         }

         @Override
         public void run()
         {
            try
            {
               SessionImpl session2 = (SessionImpl)repository.login(credentials, "ws");
               Node rootNode = session2.getRootNode();
               rootNode.addNode("test");
               assertTrue(session2.isLive());

               if (active)
               {
                  log.info("start active session");
                  long startTime = System.currentTimeMillis();
                  while (startTime + sessionRegistry.timeOut * 2 < System.currentTimeMillis())
                  {
                     Node root2 = session2.getRootNode();
                     Node testNode = root2.getNode("test");
                     testNode.setProperty("prop1", "value");
                     Thread.sleep(sessionRegistry.timeOut / 2);
                  }
                  result = session2.isLive();
               }
               else
               {
                  log.info("start pasive session");
                  Thread.sleep(SessionRegistry.DEFAULT_CLEANER_TIMEOUT + 1000);
                  result = !session2.isLive();
               }

            }
            catch (InterruptedException e)
            {
            }
            catch (LoginException e)
            {
            }
            catch (NoSuchWorkspaceException e)
            {
            }
            catch (RepositoryException e)
            {
            }
         }

      }
      List<Agent> agents = new ArrayList<Agent>();
      for (int i = 0; i < AGENT_COUNT; i++)
      {
         agents.add(new Agent());
      }
      for (Agent agent : agents)
      {
         agent.start();
      }
      boolean isNeedWait = true;
      while (isNeedWait)
      {
         isNeedWait = false;
         for (int i = 0; i < AGENT_COUNT; i++)
         {
            Agent curClient = agents.get(i);
            if (curClient.isAlive())
            {
               isNeedWait = true;
               break;
            }
         }
         Thread.sleep(100);
      }
      for (Agent agent2 : agents)
      {
         assertTrue(agent2.result);
      }
   }
}
