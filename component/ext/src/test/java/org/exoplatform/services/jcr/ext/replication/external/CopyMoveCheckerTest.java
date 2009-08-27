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
package org.exoplatform.services.jcr.ext.replication.external;

import org.exoplatform.services.jcr.ext.replication.test.ReplicationTestService;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: CopyMoveCheckerTest.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class CopyMoveCheckerTest extends BaseTestCaseChecker
{

   static long[] filesSize = new long[]{12314, 2106584, 305682};

   public void testWorkspaceCopy() throws Exception
   {
      String srcRepoPathArray[] = new String[filesSize.length];
      String nodeNameArray[] = new String[filesSize.length];
      String destNodeNameArray[] = new String[filesSize.length];

      // copy nt:file in masterMember
      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < filesSize.length; i++)
      {
         long contentSize = filesSize[i];
         String srcRepoPath = createRelPath(contentSize);
         srcRepoPathArray[i] = srcRepoPath;
         nodeNameArray[i] = "source_nt_file_" + contentSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);
         destNodeNameArray[i] = "destination_nt_file_" + contentSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + srcRepoPath + "/"
               + nodeNameArray[i] + "/" + destNodeNameArray[i] + "/" + contentSize + "/"
               + ReplicationTestService.Constants.OperationType.WORKSPACE_COPY;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember, 4000);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // check copy node in slaveMember

      for (int i = 0; i < filesSize.length; i++)
      {
         long contentSize = filesSize[i];
         String srcRepoPath = srcRepoPathArray[i];

         for (MemberInfo slaveMember : getCurrentSlaveMembers())
         {
            String checkUrl =
               "http://" + slaveMember.getIpAddress() + ":" + slaveMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + slaveMember.getLogin() + "/" + slaveMember.getPassword() + "/" + srcRepoPath + "/"
                  + nodeNameArray[i] + "/" + destNodeNameArray[i] + "/" + contentSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_COPY_MOVE_NODE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(slaveMember, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }

   public void testSessionMove() throws Exception
   {
      String srcRepoPathArray[] = new String[filesSize.length];
      String nodeNameArray[] = new String[filesSize.length];
      String destNodeNameArray[] = new String[filesSize.length];

      // move nt:file in masterMember
      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < filesSize.length; i++)
      {
         long contentSize = filesSize[i];
         String srcRepoPath = createRelPath(contentSize);
         srcRepoPathArray[i] = srcRepoPath;
         nodeNameArray[i] = "source_nt_file_" + contentSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);
         destNodeNameArray[i] = "destination_nt_file_" + contentSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + srcRepoPath + "/"
               + nodeNameArray[i] + "/" + destNodeNameArray[i] + "/" + contentSize + "/"
               + ReplicationTestService.Constants.OperationType.SESSION_MOVE;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember, 4000);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // check moved node in slaveMember

      for (int i = 0; i < filesSize.length; i++)
      {
         long contentSize = filesSize[i];
         String srcRepoPath = srcRepoPathArray[i];

         for (MemberInfo slaveMember : getCurrentSlaveMembers())
         {
            String checkUrl =
               "http://" + slaveMember.getIpAddress() + ":" + slaveMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + slaveMember.getLogin() + "/" + slaveMember.getPassword() + "/" + srcRepoPath + "/"
                  + nodeNameArray[i] + "/" + destNodeNameArray[i] + "/" + contentSize + "/"
                  + ReplicationTestService.Constants.OperationType.CHECK_COPY_MOVE_NODE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(slaveMember, 4000);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }
}
