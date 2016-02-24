/*
 * Copyright (C) 2016 eXo Platform SAS.
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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: Body Header.java 34027 2009-07-15 23:26:43Z aheritier $
 */
public class JCRBugTest extends JcrAPIBaseTest
{
   private static final int CHILDS_COUNT = 20;
   private static final int THREAD_COUNT = CHILDS_COUNT;

   private ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
   private AtomicInteger counter = new AtomicInteger(0);

   public void testUpload() throws Exception {

         try {
            // Create new parent file
            final String parentFolderPath = createParentFolder();

            // execute query to get the list of sub files (= 0)
            assertEquals(getResultSize(parentFolderPath), 0);

            for (int j = 0; j < CHILDS_COUNT; j++) {
               final int index = j;
               executorService.execute(new Runnable() {
                  public void run() {
                     try {
                        saveChild(parentFolderPath, "child" + index);
                     } catch (Exception e) {
                        e.printStackTrace();
                        fail("Error while adding child node: " + e.getMessage());
                     } finally {
                        counter.incrementAndGet();
                     }
                  }
               });
            }

            // Wait until files are uploaded
            do {
               Thread.sleep(300);
            } while (counter.get() < CHILDS_COUNT);

            // execute query to get the list of sub files (= FILES_COUNT)
            assertEquals(getResultSize(parentFolderPath), CHILDS_COUNT);

         } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
         }
   }

   private long getResultSize(String parentFolderPath) throws Exception {
      Session session = repository.getSystemSession(workspace.getName());
      String queryStatement = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + parentFolderPath + "/%'";
      Query query = session.getWorkspace().getQueryManager().createQuery(queryStatement, Query.SQL);
      return query.execute().getNodes().getSize();
   }

   private String createParentFolder() throws Exception {
      String parentFolderPath = null;
      Session session = repository.getSystemSession(workspace.getName());

      Node rootParentNode = session.getRootNode();
      Node node = rootParentNode.addNode("parents");
      session.save();
      return node.getPath();
   }

   private void saveChild(String parentPath, String fileName) throws Exception {
      Session session = repository.getSystemSession(workspace.getName());
      Node parent = (Node) session.getItem(parentPath);
      parent.addNode(fileName);
      System.out.println("Begin "+Thread.currentThread().getName()+" : "+fileName);
      session.save();
      System.out.println("End "+Thread.currentThread().getName()+" : "+fileName);
      session.refresh(false);
   }

   protected void tearDown() throws Exception {}
}
