/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.cluster.functional;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class TestEditedParentSearch extends JcrImplBaseTest
{
   /**
    * Maximum number of nodes.
    */
   private static final int MAX_NODES_COUNT = 1000;

   private static final String TEST_ROOT = "TestEditedParentSearch";

   private static final String PROP1_NAME = "p1";

   private static final String PROP2_NAME = "p2";

   private static final String PROP1_VALUE = "v1";

   private static final String PROP2_VALUE = "v2";

   private List<String> paths;

   /**
    * Test Eduted parent search.
    * @throws Exception
    */
   public void testEditedParentSearch() throws Exception
   {
      Node testRoot = root.addNode(TEST_ROOT);
      session.save();
      paths = new ArrayList<String>();

      System.out.println("Initial (y/n) :");
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String line = reader.readLine();
      if (line.equals("y"))
      {

         for (int i = 0; i < MAX_NODES_COUNT; i++)
         {
            Node nodel1 = testRoot.addNode("NODE_L1_" + i);
            Node nodel2 = nodel1.addNode("Node_L2");
            nodel2.setProperty(PROP2_NAME, PROP2_VALUE);
            session.save();
            paths.add(nodel2.getPath());
         }
      }
      else
      {
         for (int i = 0; i < MAX_NODES_COUNT; i++)
         {
            paths.add("/" + TEST_ROOT + "/" + "NODE_L1_" + i + "/" + "Node_L2");
         }

      }
      Thread searchAgent = new Thread(new SearchAgent());
      searchAgent.setName("searchAgent");
      Thread editAgent = new Thread(new EditAgent());
      editAgent.setName("editAgent");
      editAgent.start();
      searchAgent.start();

      Thread.sleep(60 * 60 * 1000);
   }

   private class EditAgent implements Runnable
   {

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {

         CredentialsImpl agentCredentials = new CredentialsImpl("admin", "admin".toCharArray());
         Random random = new Random();

         try
         {
            SessionImpl editSession = (SessionImpl)repository.login(agentCredentials, "ws");
            while (true)
            {

               ItemImpl item = editSession.getItem(paths.get(random.nextInt(paths.size())));
               NodeImpl parentNode = item.getParent();
               parentNode.setProperty(PROP1_NAME, PROP1_VALUE);
               editSession.save();
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   private class SearchAgent implements Runnable
   {

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         try
         {
            CredentialsImpl agentCredentials = new CredentialsImpl("admin", "admin".toCharArray());
            SessionImpl searchSession = (SessionImpl)repository.login(agentCredentials, "ws");
            while (true)
            {

               Node testRoot = searchSession.getRootNode().getNode(TEST_ROOT);
               // prepare nodes

               Query q =
                  searchSession.getWorkspace().getQueryManager().createQuery(
                     "SELECT * FROM nt:base WHERE " + PROP2_NAME + "='" + PROP2_VALUE + "' AND jcr:path LIKE '"
                        + testRoot.getPath() + "/%'", Query.SQL);
               long start = System.currentTimeMillis();
               QueryResult res = q.execute();
               long sqlsize = res.getNodes().getSize();
               if (sqlsize == MAX_NODES_COUNT)
               {
                  log.info("size=" + sqlsize + " time=" + (System.currentTimeMillis() - start));
               }
               else
               {
                  log.warn("!!!!!!!!!!! size=" + sqlsize + " time=" + (System.currentTimeMillis() - start));
               }
            }

         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

      }
   }

}
