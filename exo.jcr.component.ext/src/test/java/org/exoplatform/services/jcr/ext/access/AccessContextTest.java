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

package org.exoplatform.services.jcr.ext.access;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class AccessContextTest extends BaseStandaloneTest
{

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.AccessContextTest");

   private final static int MULTI_THIARD_OPERATIONS = 100;

   private final static int THREAD_COUNT = 300;

   public void testAccessMenedgerContextMultiThread() throws RepositoryException, InterruptedException
   {

      Node multiACTNode = root.addNode("testMultiACT");

      Random random = new Random();
      int nextInt = 0;
      for (int i = 0; i < MULTI_THIARD_OPERATIONS; i++)
      {
         nextInt = random.nextInt(100);
         if (nextInt % 2 == 0)
         {
            multiACTNode.setProperty("deny" + i, i);
         }
         else
         {
            multiACTNode.setProperty("someNode" + i, i);
         }
      }
      session.save();
      // Run each thread
      ArrayList<JCRClient4AccessContext> clients = new ArrayList<JCRClient4AccessContext>();

      for (int i = 0; i < THREAD_COUNT; i++)
      {
         JCRClient4AccessContext jcrClient = new JCRClient4AccessContext();
         jcrClient.start();
         clients.add(jcrClient);
      }
      // Next code is waiting for shutting down of all the threads
      boolean isNeedWait = true;
      while (isNeedWait)
      {
         isNeedWait = false;
         for (int i = 0; i < THREAD_COUNT; i++)
         {
            JCRClient4AccessContext curClient = clients.get(i);
            if (curClient.isAlive())
            {
               isNeedWait = true;
               break;
            }
         }
         Thread.sleep(100);
      }
   }

   protected class JCRClient4AccessContext extends Thread
   {
      private SessionImpl systemSession;

      private SessionImpl adminSession;

      private SessionImpl userSession;

      private Log log = ExoLogger.getLogger("exo.jcr.component.ext.JCRClient4AccessContextTest");

      public JCRClient4AccessContext()
      {
         try
         {

            systemSession = repository.getSystemSession();
            adminSession = repository.getSystemSession();
            userSession = (SessionImpl)repository.login(credentials, "ws");
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

      private void SequentialReadProperty() throws RepositoryException
      {
         Node sysNode = systemSession.getRootNode().getNode("testMultiACT");
         Node adminNode = adminSession.getRootNode().getNode("testMultiACT");
         Node userNode = userSession.getRootNode().getNode("testMultiACT");

         for (PropertyIterator i = userNode.getProperties(); i.hasNext();)
         {
            i.nextProperty();
         }
      }

      private void SequentialReadNode() throws RepositoryException
      {
         Node sysNode = systemSession.getRootNode().getNode("testMultiACT");
         Node adminNode = systemSession.getRootNode().getNode("testMultiACT");
         Node userNode = systemSession.getRootNode().getNode("testMultiACT");

         for (NodeIterator i = sysNode.getNodes(); i.hasNext();)
         {
            i.nextNode();
         }
         for (PropertyIterator i = adminNode.getProperties(); i.hasNext();)
         {
            Property prop = i.nextProperty();
            try
            {
               prop.getValue().getString();
            }
            catch (RepositoryException e)
            {
               log.error("Exception must not to throw");
            }
         }
         for (PropertyIterator i = userNode.getProperties(); i.hasNext();)
         {
            Property prop = i.nextProperty();
            try
            {
               prop.getValue().getString();
               if (prop.getName().indexOf("deny") > -1)
               {
                  log.error("Exception must throw");
               }
            }
            catch (RepositoryException e)
            {
            }
         }
      }

      private void ParalelRead()
      {
         for (int i = 0; i < MULTI_THIARD_OPERATIONS; i++)
         {

         }

      }

      @Override
      public void run()
      {
         try
         {
            SequentialReadProperty();
            // SequentialReadNode();
            ParalelRead();

         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
            log.error("Error");
         }
      }

   }
}
