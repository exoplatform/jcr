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
package org.exoplatform.services.jcr.load.perf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 20.07.2006
 * 
 * @version $Id: TestBulkItemsAdd.java 352 2009-10-23 09:28:36Z pnedonosko $
 */
public class TestBulkItemsAdd extends JcrAPIBaseTest
{

   public void testNodeAdd() throws Exception
   {
      StringBuilder stats = new StringBuilder();

      Node testRoot = session.getRootNode().addNode("testRoot");
      session.save();
      long startTime = System.currentTimeMillis();
      int nodesCount = 10000;
      for (int i = 0; i < nodesCount; i++)
      {
         long addTime = System.currentTimeMillis();
         String nodeName = "_" + i + "_node";
         Node n = testRoot.addNode(nodeName);
         //log.info("add node " + nodeName + ", " + (System.currentTimeMillis() - addTime) + "ms, "
         //   + (finishTime - startTime) + "ms");
         //String stat = (System.currentTimeMillis() - addTime) + ";" + (finishTime - startTime);
         if ((i % 100) == 0)
         {
            stats.append(i);
            stats.append(";");
            stats.append((System.currentTimeMillis() - addTime));
            //            stats.append(";");
            //            stats.append((System.currentTimeMillis() - startTime));
            stats.append("\r\n");
         }
      }
      //      stats.append(nodesCount);
      //      stats.append(";");
      //      stats.append((System.currentTimeMillis() - startTime));
      //      stats.append("\r\n");

      log.info("Nodes added " + nodesCount + ", " + (System.currentTimeMillis() - startTime) + "ms");
      startTime = System.currentTimeMillis();
      log.info("Nodes will be saved, wait few minutes...");
      testRoot.save();
      log.info("Nodes saved " + nodesCount + ", " + (System.currentTimeMillis() - startTime) + "ms");

      // save stats
      PrintWriter statFile = new PrintWriter(new File("stats.txt"));
      statFile.write(stats.toString());
      statFile.close();

      log.info("Test done");
   }

   public void _testNtFileAdd() throws Exception
   {
      Node testRoot = session.getRootNode().addNode("testRoot");
      session.save();
      long startTime = System.currentTimeMillis();
      int nodesCount = 250;
      for (int i = 0; i < nodesCount; i++)
      {
         long addTime = System.currentTimeMillis();
         String nodeName = "_" + i + "_ntfile";

         Node n = testRoot.addNode(nodeName, "nt:file");
         Node nContent = n.addNode("jcr:content", "nt:unstructured");
         nContent.setProperty("currenTime", Calendar.getInstance());
         nContent.setProperty("info", "Info string");
         Node resource = nContent.addNode("fileData", "nt:resource");
         resource.setProperty("jcr:mimeType", "text/plain");
         resource.setProperty("jcr:lastModified", Calendar.getInstance());
         resource.setProperty("jcr:data", new ByteArrayInputStream("Some bin data;asdasdasdasdeqecvsdfvdf".getBytes()));

         log.info("add node " + nodeName + ", " + (System.currentTimeMillis() - addTime) + "ms, "
            + (System.currentTimeMillis() - startTime) + "ms");
      }
      log.info("Nodes (nt:file) added " + nodesCount + ", " + (System.currentTimeMillis() - startTime) + "ms");
      startTime = System.currentTimeMillis();
      log.info("Nodes (nt:file) will be saved, wait few minutes...");
      testRoot.save();
      log.info("Nodes (nt:file) saved " + nodesCount + ", " + (System.currentTimeMillis() - startTime) + "ms");
   }

}
