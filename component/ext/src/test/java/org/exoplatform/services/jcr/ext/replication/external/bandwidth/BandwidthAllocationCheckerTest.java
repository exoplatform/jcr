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
package org.exoplatform.services.jcr.ext.replication.external.bandwidth;

import org.exoplatform.services.jcr.ext.replication.external.BaseTestCaseChecker;
import org.exoplatform.services.jcr.ext.replication.external.BasicAuthenticationHttpClient;
import org.exoplatform.services.jcr.ext.replication.external.MemberInfo;
import org.exoplatform.services.jcr.ext.replication.test.ReplicationTestService;

/**
 * Created by The eXo Platform SAS
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: BandwidthAllocationCheckerTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
/**
 * @author rainf0x
 * 
 */
public class BandwidthAllocationCheckerTest extends BaseTestCaseChecker
{

   public void testAddEmptyNode() throws Exception
   {
      // add the 10-th base node
      int baseNodesCount = 100;
      int simpleOperationCount = 100;
      long start, end;

      String repoPathArray[] = new String[baseNodesCount];
      String nodeNameArray[] = new String[baseNodesCount];

      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < baseNodesCount; i++)
      {

         String repoPath = createRelPath(getRandomLong() % 5);
         repoPathArray[i] = repoPath;
         nodeNameArray[i] = "n" + i;

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPath + "/" + nodeNameArray[i]
               + "/" + ReplicationTestService.Constants.OperationType.CREATE_BASE_NODE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // sleep 60 seconds
      System.out.println("Sleep 60 seconds ...");
      Thread.sleep(60 * 1000);

      System.out.println("Stat the empty nodes adding ...");

      start = System.currentTimeMillis(); // to get the time of start

      // add empty nodes (10*100 == 1000)
      for (int i = 0; i < baseNodesCount; i++)
      {
         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPathArray[i] + "/"
               + nodeNameArray[i] + "/" + simpleOperationCount + "/"
               + ReplicationTestService.Constants.OperationType.ADD_EMPTY_NODE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      end = System.currentTimeMillis();

      System.out.println("The time of the adding the empty nodes (" + baseNodesCount * simpleOperationCount + ") : "
         + ((end - start) / 1000) + " sec");

   }

   public void testAddStringProperty() throws Exception
   {
      // add the 10-th base node
      int baseNodesCount = 1000;
      int simpleOperationCount = 1;
      int stringSize = 128;
      long start, end;

      String repoPathArray[] = new String[baseNodesCount];
      String nodeNameArray[] = new String[baseNodesCount];

      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < baseNodesCount; i++)
      {

         String repoPath = createRelPath(getRandomLong() % 4);
         repoPathArray[i] = repoPath;
         nodeNameArray[i] = "n" + i;

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPath + "/" + nodeNameArray[i]
               + "/" + ReplicationTestService.Constants.OperationType.CREATE_BASE_NODE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // sleep 60 seconds
      System.out.println("Sleep 60 seconds ...");
      Thread.sleep(60 * 1000);

      System.out.println("Stat the " + stringSize + "B string property adding ...");

      start = System.currentTimeMillis(); // to get the time of start

      // add 128B string property (10*100 == 1000)
      for (int i = 0; i < baseNodesCount; i++)
      {
         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPathArray[i] + "/"
               + nodeNameArray[i] + "/" + stringSize + "/" + simpleOperationCount + "/"
               + ReplicationTestService.Constants.OperationType.ADD_STRING_PROPETY_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      end = System.currentTimeMillis();

      System.out.println("The time of the adding the string property (" + baseNodesCount * simpleOperationCount
         + ") : " + ((end - start) / 1000) + " sec");

   }

   public void testAddBinaryProperty() throws Exception
   {
      // add the 10-th base node
      int baseNodesCount = 1000;
      int simpleOperationCount = 1;
      int binarySize = 1024 * 1024;
      long start, end;

      String repoPathArray[] = new String[baseNodesCount];
      String nodeNameArray[] = new String[baseNodesCount];

      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < baseNodesCount; i++)
      {

         String repoPath = createRelPath(getRandomLong() % 6);
         repoPathArray[i] = repoPath;
         nodeNameArray[i] = "n" + i;

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPath + "/" + nodeNameArray[i]
               + "/" + ReplicationTestService.Constants.OperationType.CREATE_BASE_NODE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // sleep 60 seconds
      System.out.println("Sleep 60 seconds ...");
      Thread.sleep(60 * 1000);

      System.out.println("Stat the " + binarySize + "B binary property adding ...");

      start = System.currentTimeMillis(); // to get the time of start

      // add 128B string property (10*100 == 1000)
      for (int i = 0; i < baseNodesCount; i++)
      {
         // Thread.sleep(300);
         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + repoPathArray[i] + "/"
               + nodeNameArray[i] + "/" + binarySize + "/" + simpleOperationCount + "/"
               + ReplicationTestService.Constants.OperationType.ADD_BINARY_PROPERTY_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      end = System.currentTimeMillis();

      System.out.println("The time of the adding the string property (" + baseNodesCount * simpleOperationCount
         + ") : " + ((end - start) / 1000) + " sec");

   }
}
