/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestErrorMultithreading extends BaseQueryTest
{
   public static final int COUNT = 5;

   public static final int NODE_COUNT = 10;

   public static final int THREADS_COUNT = 10;

   public static final String THREAD_NAME = "name";

   @Override
   public void tearDown()
   {

   }

   public void testRunActions() throws Exception
   {
      // fillRepo();
      // checkRepo();
      // checkRepoByContent();
      loadLargeFiles();
   }

   private void loadLargeFiles() throws Exception
   {
      class Writer extends Thread
      {
         public String name;

         public Session sess;

         File file = PrivilegedFileHelper.file("src/test/resources/LARGE.txt");

         public FileInputStream fis = null;

         Writer(String name, Session s)
         {
            this.name = name;
            this.sess = s;
         }

         @Override
         public void run()
         {
            System.out.println(name + " - START");
            try
            {
               Node root = sess.getRootNode();
               for (int i = 0; i < 100000; i++)
               {

                  IdGenerator obj;
                  String fileName = IdGenerator.generate();// + "_" + i;
                  NodeImpl node = (NodeImpl)root.addNode(fileName, "nt:file");
                  NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
                  cont.setProperty("jcr:mimeType", "text/plain");
                  cont.setProperty("jcr:lastModified", Calendar.getInstance());
                  fis = PrivilegedFileHelper.fileInputStream(file);
                  cont.setProperty("jcr:data", fis);
                  root.save();
                  System.out.println(fileName + " saved");
                  fis.close();
               }
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
            System.out.println(name + " - STOP");
         }
      }

      Set<Writer> writers = new HashSet<Writer>();

      // create
      for (int t = 0; t < 10; t++)
      {
         Credentials credentials = new CredentialsImpl("admin", "admin".toCharArray());

         Session ss = repository.login(credentials, "ws");
         Writer wr = new Writer(THREAD_NAME + t, ss);
         writers.add(wr);
      }

      // start
      Iterator<Writer> it = writers.iterator();
      while (it.hasNext())
      {
         it.next().start();
      }

      // join
      it = writers.iterator();
      while (it.hasNext())
      {
         it.next().join();
      }

   }

   private void fillRepo() throws Exception
   {

      class Writer extends Thread
      {
         public String name;

         public Session sess;

         Writer(String name, Session s)
         {
            this.name = name;
            this.sess = s;

            super.setName(this.name); // super.getName() + ", " +
         }

         @Override
         public void run()
         {
            System.out.println(name + " - START");
            try
            {
               Node root = sess.getRootNode();

               for (int i = 0; i < COUNT; i++)
               {
                  for (int j = 0; j < NODE_COUNT; j++)
                  {
                     int num = i * NODE_COUNT * 10 + j;
                     String n = name + "_" + num;
                     root.addNode(n);
                     System.out.println("ADD " + n);
                  }
                  root.save();
                  System.out.println(name + " - SAVE");
               }
            }
            catch (Exception e)
            {
               System.out.println();
               System.out.println(name + "-thread error");
               e.printStackTrace();
            }
            System.out.println(name + " - FINISH");
         }
      }

      Set<Writer> writers = new HashSet<Writer>();

      // create
      for (int t = 0; t < THREADS_COUNT; t++)
      {
         Credentials credentials = new CredentialsImpl("admin", "admin".toCharArray());

         Session ss = repository.login(credentials, "ws");
         Writer wr = new Writer(THREAD_NAME + t, ss);
         writers.add(wr);
      }

      // start
      Iterator<Writer> it = writers.iterator();
      while (it.hasNext())
      {
         it.next().start();
      }

      // join
      it = writers.iterator();
      while (it.hasNext())
      {
         it.next().join();
      }

      System.out.println("FINISH!");
      Object obj = new Object();
      synchronized (obj)
      {
         try
         {
            obj.wait(10000);
         }
         catch (Exception e)
         {

         }
      }
   }

   private void checkRepo() throws Exception
   {
      QueryManager qman = this.workspace.getQueryManager();

      for (int t = 0; t < THREADS_COUNT; t++)
      {
         String name = THREAD_NAME + t;

         for (int i = 0; i < COUNT; i++)
         {
            for (int j = 0; j < NODE_COUNT; j++)
            {
               int num = i * NODE_COUNT * 10 + j;
               String n = name + "_" + num;
               Query q = qman.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + n + "'", Query.SQL);
               QueryResult res = q.execute();

               if (res.getNodes().getSize() != 1)
               {
                  System.out.println("Thread " + t + "  " + n + " NO");
               }
            }
         }
      }
   }

   private void checkRepoByContent() throws Exception
   {
      QueryManager qman = this.workspace.getQueryManager();
      Node root = session.getRootNode();
      NodeIterator it = root.getNodes();
      System.out.append("SEARCH START");
      System.out.println("Nodes: " + it.getSize());
      while (it.hasNext())
      {
         Node node = it.nextNode();
         String name = node.getName();
         Query q = qman.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + name + "'", Query.SQL);
         QueryResult res = q.execute();

         if (res.getNodes().getSize() != 1)
         {
            System.out.println(name + " NO");
         }
      }
      System.out.append("SEARCH STOP");

   }
}
