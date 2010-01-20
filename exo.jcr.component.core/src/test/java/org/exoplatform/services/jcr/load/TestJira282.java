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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestJira282.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestJira282 extends JcrAPIBaseTest
{
   private final static int ADD_THREAD_COUNT = 1;

   private final static int GET_THREAD_COUNT = 5;

   private final static int SET_THREAD_COUNT = 1;

   private final static int LEVELE1_NODES_COUNT = 1000;

   private final static int LEVELE2_NODES_COUNT = 10;

   private static final List<String> validNames = Collections.synchronizedList(new ArrayList<String>());

   private static final Set<String> hotNodes = Collections.synchronizedSet(new HashSet<String>());

   private static final int MAX_VALID_NAMES_LIST_SIZE = 500000;

   private static final int MAX_DEPTH = 2;

   private static final int TEST_TIME = 60 * 60 * 1000; // 1

   // min

   private static final Random random = new Random();

   // static {
   // validNames.add("/");
   // }

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.TestJira282");

   public void test283() throws Exception
   {
      List<Agent> agents = new ArrayList<Agent>();

      prepareRoot();

      for (int i = 0; i < ADD_THREAD_COUNT; i++)
      {
         AddAgent agent =
            new AddAgent((SessionImpl)repository.login(this.credentials, session.getWorkspace().getName()));
         agent.start();
         agents.add(agent);
      }
      for (int i = 0; i < GET_THREAD_COUNT; i++)
      {
         GetAgent agent =
            new GetAgent((SessionImpl)repository.login(this.credentials, session.getWorkspace().getName()));
         agent.start();
         agents.add(agent);
      }
      for (int i = 0; i < SET_THREAD_COUNT; i++)
      {
         SetAgent agent =
            new SetAgent((SessionImpl)repository.login(this.credentials, session.getWorkspace().getName()));
         agent.start();
         agents.add(agent);
      }

      Thread.sleep(TEST_TIME);

      for (Agent thread : agents)
      {
         // agents.ge
         thread.getAgentSession().logout();
         thread.interrupt();
      }
   }

   private void prepareRoot() throws RepositoryException
   {
      Node root1 = session.getRootNode().addNode("node1");

      for (int i = 0; i < LEVELE1_NODES_COUNT; i++)
      {
         Node root2 = root1.addNode(IdGenerator.generate());
         int max = 10;// random.nextInt(1000);
         log.info("Node " + i + " max:" + max);
         for (int j = 0; j < LEVELE2_NODES_COUNT; j++)
         {
            Node node = root2.addNode(IdGenerator.generate());
         }
         session.save();
         validNames.add(root2.getPath());
      }
   }

   private class AddAgent extends Agent
   {
      private final SessionImpl agentSession;

      private final Log log = ExoLogger.getLogger("jcr.AddAgent");

      public AddAgent(SessionImpl agentSession)
      {
         super();
         this.agentSession = agentSession;
      }

      /*
       * (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      /**
       * @return the agentSession
       */
      @Override
      public SessionImpl getAgentSession()
      {
         return agentSession;
      }

      @Override
      public void run()
      {
         setName("AddAgent " + getId());
         while (!isInterrupted())
         {
            String validName = validNames.get(random.nextInt(validNames.size()));
            try
            {
               for (int i = 0; i < LEVELE2_NODES_COUNT; i++)
               {
                  Node node = (Node)agentSession.getItem(validName);
                  Node newNode = node.addNode(IdGenerator.generate());
                  agentSession.save();

               }
               validNames.add(validName);
               // synchronized (validNames) {
               // if (validNames.size() < MAX_VALID_NAMES_LIST_SIZE &&
               // newNode.getDepth() < MAX_DEPTH)
               // validNames.add(newNode.getPath());
               // }
               // log.info("Node:" + newNode.getPath() + " added");
            }
            catch (PathNotFoundException e)
            {
               e.printStackTrace();
            }
            catch (RepositoryException e)
            {
               e.printStackTrace();
            }

         }
         log.info("Finish");
      }

   }

   private abstract class Agent extends Thread
   {
      public abstract SessionImpl getAgentSession();
   }

   private class GetAgent extends Agent
   {
      private final SessionImpl agentSession;

      private final Log log = ExoLogger.getLogger("jcr.GetAgent");

      public GetAgent(SessionImpl agentSession)
      {
         super();
         this.agentSession = agentSession;
      }

      @Override
      public SessionImpl getAgentSession()
      {
         return agentSession;
      }

      /*
       * (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         setName("GetAgent " + getId());
         while (!isInterrupted())
         {
            String validName = validNames.get(random.nextInt(validNames.size()));
            try
            {
               NodeImpl node = (NodeImpl)agentSession.getItem(validName);
               for (NodeIterator iter = node.getNodes(); iter.hasNext();)
               {
                  String name = iter.nextNode().getPath();
               }
            }
            catch (PathNotFoundException e)
            {
               e.printStackTrace();
            }
            catch (RepositoryException e)
            {
               e.printStackTrace();
            }
         }
         log.info("Finish");
      }
   }

   private class SetAgent extends Agent
   {
      private final SessionImpl agentSession;

      private final Log log = ExoLogger.getLogger("jcr.SetAgent");

      public SetAgent(SessionImpl agentSession)
      {
         super();
         this.agentSession = agentSession;
      }

      @Override
      public SessionImpl getAgentSession()
      {
         return agentSession;
      }

      /*
       * (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         setName("SetAgent " + getId());
         while (!isInterrupted())
         {
            String validName = validNames.get(random.nextInt(validNames.size()));
            if (hotNodes.contains(validName))
               continue;
            try
            {
               hotNodes.add(validName);
               NodeImpl node = (NodeImpl)agentSession.getItem(validName);
               node.setProperty("testField", IdGenerator.generate());
               agentSession.save();
               hotNodes.remove(validName);
               // log.info("Update node " + validName);
            }
            catch (PathNotFoundException e)
            {
               e.printStackTrace();
            }
            catch (RepositoryException e)
            {
               e.printStackTrace();
            }
         }
         log.info("Finish");
      }
   }

}
