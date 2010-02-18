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

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;
import org.exoplatform.services.jcr.cluster.load.NodeInfo;
import org.exoplatform.services.jcr.cluster.load.WorkerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class WebDavTestAgent extends AbstractWebDavTestAgent
{

   private String testRoot;

   /**
    * @param nodesPath
    * @param responceResults
    * @param startSignal
    * @param READ_VALUE
    * @param random
    */
   public WebDavTestAgent(String testRoot, List<NodeInfo> nodesPath, List<WorkerResult> responceResults,
      CountDownLatch startSignal, int READ_VALUE, Random random)
   {
      super(nodesPath, responceResults, startSignal, READ_VALUE, random);
      this.testRoot = testRoot;
   }

   /**
    * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doRead(java.util.List)
    */
   @Override
   public void doRead(List<NodeInfo> nodesPath, List<WorkerResult> responseResults)
   {
      //List<WorkerResult> result = new ArrayList<WorkerResult>();
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
         JCRWebdavConnection conn = null;
         try
         {
            conn = getNewConnection();
            HTTPResponse response = conn.getNode(readNodePath);
            if (response.getStatusCode() == HTTPStatus.OK)
            {
               responseResults.add(new WorkerResult(true, System.currentTimeMillis() - start));
            }
            else
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
            if (conn != null)
            {
               conn.stop();
            }
         }

      }
   }

   /**
    * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#prepare()
    */
   @Override
   protected void prepare()
   {
      testRoot = createDirIfAbsent(testRoot, UUID.randomUUID().toString(), new ArrayList<WorkerResult>());
   }

   /**
    * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doWrite(java.util.List)
    */
   @Override
   public void doWrite(List<NodeInfo> nodesPath, List<WorkerResult> responseResults)
   {

      JCRWebdavConnection connection = null;
      try
      {
         connection = getNewConnection();
         String putFile =
            createDirIfAbsent(testRoot, UUID.randomUUID().toString(), new ArrayList<WorkerResult>()) + "/file";
         long start = System.currentTimeMillis();
         HTTPResponse response = connection.addNode(putFile, ("__the_data_in_nt+file__").getBytes());

         if (response.getStatusCode() == HTTPStatus.CREATED)
         {
            responseResults.add(new WorkerResult(false, System.currentTimeMillis() - start));
            nodesPath.add(new NodeInfo(putFile, System.currentTimeMillis()));
         }
         else
         {
            System.out.println(Thread.currentThread().getName() + " : Can not add (response code "
               + response.getStatusCode() + new String(response.getData()) + " ) file with path : " + putFile);

         }

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
   }

   /**
    * Create WebDav node if not exist
    * @param root
    * @param name
    * @param data
    * @return
    */
   public String createDirIfAbsent(String root, String name, List<WorkerResult> result)
   {
      String path = root.length() == 0 ? name : root + "/" + name;
      JCRWebdavConnection connection = null;
      try
      {
         connection = getNewConnection();

         long start = System.currentTimeMillis();
         HTTPResponse nodeResponce = connection.getNode(path);
         //add information about read
         result.add(new WorkerResult(true, System.currentTimeMillis() - start));
         if (nodeResponce.getStatusCode() != HTTPStatus.OK)
         {
            start = System.currentTimeMillis();
            HTTPResponse addResponce = connection.addDir(path);
            //add information about write

            if (addResponce.getStatusCode() == HTTPStatus.CREATED)
            {
               result.add(new WorkerResult(false, System.currentTimeMillis() - start));
            }
            else
            {
               System.out.println(Thread.currentThread().getName() + " : Can not add (response code "
                  + addResponce.getStatusCode() + new String(addResponce.getData()) + " ) node with path : " + path);

            }
         }
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
      return path;
   }

   /**
    * Create WebDav node if not exist
    * @param root
    * @param uuid
    * @param data
    * @return
    */
   public String createDirIfAbsent(String root, UUID uuid, List<WorkerResult> result)
   {
      String uuidPath = uuid.toString();
      String l1 = createDirIfAbsent(root, uuidPath.substring(0, 8), result);
      //      String l2 = createDirIfAbsent(l1, uuidPath.substring(9, 13), result);
      //      String l3 = createDirIfAbsent(l2, uuidPath.substring(14, 18), result);
      //      String l4 = createDirIfAbsent(l3, uuidPath.substring(19, 23), result);
      return createDirIfAbsent(l1, uuidPath.substring(9), result);

   }
}
