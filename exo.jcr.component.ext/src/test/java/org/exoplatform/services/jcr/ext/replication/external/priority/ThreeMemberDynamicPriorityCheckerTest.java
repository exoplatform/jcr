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
package org.exoplatform.services.jcr.ext.replication.external.priority;

import org.exoplatform.services.jcr.ext.replication.external.BaseTestCaseChecker;
import org.exoplatform.services.jcr.ext.replication.external.BasicAuthenticationHttpClient;
import org.exoplatform.services.jcr.ext.replication.external.MemberInfo;
import org.exoplatform.services.jcr.ext.replication.test.ReplicationTestService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ThreeMemberDynamicPriorityCheckerTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ThreeMemberDynamicPriorityCheckerTest extends BaseTestCaseChecker
{
   long[] filesSize = new long[]{12314, 652125, 5212358, 9521358};

   String relPathArray[];

   String fileNameArray[];

   int channelNameId = 7;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      relPathArray = new String[filesSize.length];
      fileNameArray = new String[filesSize.length];

      Thread.sleep(15000);
   }

   public void testDisconnectMaxAndMinPriority() throws Exception
   {
      MemberInfo maxPriorityMember = getMaxPriorityMember();

      List<MemberInfo> otherMember = new ArrayList<MemberInfo>();
      otherMember.add(getMiddlePriorityMember());
      otherMember.add(getMinPriorityMember());

      // disconnect max priority member
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/" + channelNameId
               + "/" + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE_BY_ID;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(25000);

      // check is read-only max priority member
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // add content to minPriorityMember
      for (int i = 0; i < filesSize.length - 2; i++)
      {
         long fSize = filesSize[i];
         String relPath = createRelPath(fSize);
         relPathArray[i] = relPath;
         fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + relPath + "/"
               + fileNameArray[i] + "/" + fSize + "/" + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(30 * 1000);

      // ------------------ Disconnect min priority member
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + channelNameId
               + 1 + "/" + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE_BY_ID;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(25000);

      // check is read-only min priority member
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(30 * 1000);

      // add content to middlePriority member
      for (int i = 2; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = createRelPath(fSize);
         relPathArray[i] = relPath;
         fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + getMiddlePriorityMember().getIpAddress() + ":" + getMiddlePriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMiddlePriorityMember().getLogin() + "/" + getMiddlePriorityMember().getPassword() + "/" + relPath
               + "/" + fileNameArray[i] + "/" + fSize + "/"
               + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMiddlePriorityMember());
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(60 * 1000);

      // allow connect to max priority
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(3 * 60 * 1000);

      // check is read-only max priority member
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // allow connect to min priority
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // wait 3 minutes (reconnect + restore will be finished)
      Thread.sleep(3 * 60 * 1000);

      // check nt:file in members
      randomizeMembers();

      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = relPathArray[i];

         for (MemberInfo member : getAllMembers())
         {
            String checkUrl =
               "http://" + member.getIpAddress() + ":" + member.getPort() + ReplicationTestService.Constants.BASE_URL
                  + "/" + workingRepository + "/" + workingWorkspace + "/" + member.getLogin() + "/"
                  + member.getPassword() + "/" + relPath + "/" + fileNameArray[i] + "/" + fSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_NT_FILE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(member, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }

   public void testDisconnectMinPriority() throws Exception
   {
      List<MemberInfo> otherMember = new ArrayList<MemberInfo>();
      otherMember.add(getMiddlePriorityMember());
      otherMember.add(getMinPriorityMember());

      // disconnect min priority member
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + channelNameId
               + "/" + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE_BY_ID;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(35000);

      // check is read-only min priority member
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // add content to middlPriorityMember
      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = createRelPath(fSize);
         relPathArray[i] = relPath;
         fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + getMiddlePriorityMember().getIpAddress() + ":" + getMiddlePriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMiddlePriorityMember().getLogin() + "/" + getMiddlePriorityMember().getPassword() + "/" + relPath
               + "/" + fileNameArray[i] + "/" + fSize + "/"
               + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMiddlePriorityMember());
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(60 * 1000);

      // allow connect to min priority
      {
         String disconnectUrl =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // wait 4 minutes (reconnect + restore will be finished)
      Thread.sleep(3 * 60 * 1000);

      // check nt:file in members
      randomizeMembers();

      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = relPathArray[i];

         for (MemberInfo member : getAllMembers())
         {
            String checkUrl =
               "http://" + member.getIpAddress() + ":" + member.getPort() + ReplicationTestService.Constants.BASE_URL
                  + "/" + workingRepository + "/" + workingWorkspace + "/" + member.getLogin() + "/"
                  + member.getPassword() + "/" + relPath + "/" + fileNameArray[i] + "/" + fSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_NT_FILE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(member, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }

   public void testDisconnectMaxPriority() throws Exception
   {
      MemberInfo maxPriorityMember = getMaxPriorityMember();

      List<MemberInfo> otherMember = new ArrayList<MemberInfo>();
      otherMember.add(getMiddlePriorityMember());
      otherMember.add(getMinPriorityMember());

      // disconnect max priority member
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/" + channelNameId
               + "/" + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE_BY_ID;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(35000);

      // check is read-only max priority member
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + maxPriorityMember.getLogin() + "/" + maxPriorityMember.getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // add content to minPriorityMember
      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = createRelPath(fSize);
         relPathArray[i] = relPath;
         fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + relPath + "/"
               + fileNameArray[i] + "/" + fSize + "/" + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(60 * 1000);

      // allow connect to max priority
      {
         String disconnectUrl =
            "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // wait 4 minutes (reconnect + restore will be finished)
      Thread.sleep(3 * 60 * 1000);

      // check nt:file in members
      randomizeMembers();

      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = relPathArray[i];

         for (MemberInfo member : getAllMembers())
         {
            String checkUrl =
               "http://" + member.getIpAddress() + ":" + member.getPort() + ReplicationTestService.Constants.BASE_URL
                  + "/" + workingRepository + "/" + workingWorkspace + "/" + member.getLogin() + "/"
                  + member.getPassword() + "/" + relPath + "/" + fileNameArray[i] + "/" + fSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_NT_FILE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(member, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }

   public void testDisconnectMiddlePriority() throws Exception
   {
      List<MemberInfo> otherMember = new ArrayList<MemberInfo>();
      otherMember.add(getMiddlePriorityMember());
      otherMember.add(getMinPriorityMember());

      // disconnect middle priority member
      {
         String disconnectUrl =
            "http://" + getMiddlePriorityMember().getIpAddress() + ":" + getMiddlePriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMiddlePriorityMember().getLogin() + "/" + getMiddlePriorityMember().getPassword() + "/"
               + channelNameId + "/" + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE_BY_ID;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMiddlePriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(35000);

      // check is read-only middle priority member
      {
         String disconnectUrl =
            "http://" + getMiddlePriorityMember().getIpAddress() + ":" + getMiddlePriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMiddlePriorityMember().getLogin() + "/" + getMiddlePriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_IS_READ_ONLY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMiddlePriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // add content to minPriorityMember
      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = createRelPath(fSize);
         relPathArray[i] = relPath;
         fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + relPath + "/"
               + fileNameArray[i] + "/" + fSize + "/" + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(60 * 1000);

      // allow connect to middle priority
      {
         String disconnectUrl =
            "http://" + getMiddlePriorityMember().getIpAddress() + ":" + getMiddlePriorityMember().getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + getMiddlePriorityMember().getLogin() + "/" + getMiddlePriorityMember().getPassword() + "/"
               + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMiddlePriorityMember());
         String result = client.execute(disconnectUrl);
         System.out.println(disconnectUrl);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // wait 3 minutes (reconnect + restore will be finished)
      Thread.sleep(3 * 60 * 1000);

      // check nt:file in members
      randomizeMembers();

      for (int i = 0; i < filesSize.length; i++)
      {
         long fSize = filesSize[i];
         String relPath = relPathArray[i];

         for (MemberInfo member : getAllMembers())
         {
            String checkUrl =
               "http://" + member.getIpAddress() + ":" + member.getPort() + ReplicationTestService.Constants.BASE_URL
                  + "/" + workingRepository + "/" + workingWorkspace + "/" + member.getLogin() + "/"
                  + member.getPassword() + "/" + relPath + "/" + fileNameArray[i] + "/" + fSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_NT_FILE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(member, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }

}
