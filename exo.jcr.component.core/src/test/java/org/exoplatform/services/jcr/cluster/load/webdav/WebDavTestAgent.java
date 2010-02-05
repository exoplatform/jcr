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
package org.exoplatform.services.jcr.cluster.load.webdav;

import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;
import org.exoplatform.services.jcr.cluster.load.NodeInfo;
import org.exoplatform.services.jcr.cluster.load.WorkerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class WebDavTestAgent extends AbstractWebDavTestAgent
{

   private volatile static long l1FolderCount = 0;

   private String l1FolderName;

   private long l2FolderCount;

   /**
    * @param nodesPath
    * @param responceResults
    * @param startSignal
    * @param READ_VALUE
    * @param random
    */
   public WebDavTestAgent(List<NodeInfo> nodesPath, List<WorkerResult> responceResults, CountDownLatch startSignal,
      int READ_VALUE, Random random)
   {
      super(nodesPath, responceResults, startSignal, READ_VALUE, random);
   }

   /**
    * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doRead(java.util.List)
    */
   @Override
   public List<WorkerResult> doRead(List<NodeInfo> nodesPath)
   {
      List<WorkerResult> result = new ArrayList<WorkerResult>();
      if (nodesPath.size() > 0)
      {

         String readNodePath = null;
         while (readNodePath == null)
         {
            NodeInfo nodeInfo = nodesPath.get(random.nextInt(nodesPath.size()));
            //               if ((System.currentTimeMillis() - nodeInfo.created) > 30000)
            //               {
            readNodePath = nodeInfo.getPath();
            //               }

         }
         long start = System.currentTimeMillis();
         JCRWebdavConnection conn = getNewConnection();
         try
         {
            HTTPResponse response = conn.getNode(readNodePath);
            if (response.getStatusCode() != 200)
            {
               System.out.println("Can not get (response code " + response.getStatusCode()
                  + new String(response.getData()) + " ) node with path : " + readNodePath);
            }

         }
         catch (Exception e)
         {
            System.out.println(e.getLocalizedMessage());
         }
         finally
         {
            conn.stop();
         }

         result.add(new WorkerResult(true, System.currentTimeMillis() - start));

      }
      return result;
   }

   /**
    * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doWrite(java.util.List)
    */
   @Override
   public List<WorkerResult> doWrite(List<NodeInfo> nodesPath)
   {
      List<WorkerResult> result = new ArrayList<WorkerResult>();
      long start = 0;
      JCRWebdavConnection connection = null;
      try
      {
         connection = getNewConnection();

         if (l1FolderName == null || l2FolderCount == 100)
         {
            l1FolderName = "folder" + (l1FolderCount++);
            start = System.currentTimeMillis();
            connection.addDir(l1FolderName);
            l2FolderCount = 0;
            result.add(new WorkerResult(false, System.currentTimeMillis() - start));
         }
         String path = l1FolderName + "/" + "node" + l2FolderCount++;
         start = System.currentTimeMillis();
         HTTPResponse response = connection.addNode(path, ("__the_data_in_nt+file__" + l2FolderCount).getBytes());

         if (response.getStatusCode() != 201)
         {
            System.out.println(Thread.currentThread().getName() + " : Can not add (response code "
               + response.getStatusCode() + new String(response.getData()) + " ) node with path : " + path);
         }
         result.add(new WorkerResult(false, System.currentTimeMillis() - start));
         nodesPath.add(new NodeInfo(path, System.currentTimeMillis()));
      }
      catch (Exception e)
      {
         System.out.println(e.getLocalizedMessage());
      }
      finally
      {
         if (connection != null)
         {
            connection.stop();
         }
      }
      return result;
   }
}
