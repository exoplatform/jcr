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

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: StaticPriorityCheckerTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TwoMemberPriorityCheckerTest extends BaseTestCaseChecker
{

   public void testDisconnectMaxPriority() throws Exception
   {
      for (int iteration = 0; iteration < 1; iteration++)
      {
         System.out.println("Begin iteration #" + iteration);

         long[] filesSize = new long[]{12314, 652125, 5212358, 21906584};
         String relPathArray[] = new String[filesSize.length];
         String fileNameArray[] = new String[filesSize.length];

         MemberInfo maxPriorityMember = getMaxPriorityMember();
         MemberInfo minPriorityMember = getMinPriorityMember();

         // disconnect minPriorityMember
         {
            String disconnectUrl =
               "http://" + minPriorityMember.getIpAddress() + ":" + minPriorityMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + minPriorityMember.getLogin() + "/" + minPriorityMember.getPassword() + "/"
                  + ReplicationTestService.Constants.OperationType.DISCONNECT_CLUSTER_NODE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(minPriorityMember);
            String result = client.execute(disconnectUrl);
            System.out.println(disconnectUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }

         Thread.sleep(10000);

         // add content to maxPriorityMember
         for (int i = 0; i < filesSize.length; i++)
         {
            long fSize = filesSize[i];
            String relPath = createRelPath(fSize);
            relPathArray[i] = relPath;
            fileNameArray[i] = "nt_file_" + fSize + "_" + (int)(Math.random() * MAX_RANDOM_VALUE);

            String url =
               "http://" + maxPriorityMember.getIpAddress() + ":" + maxPriorityMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + maxPriorityMember.getLogin() + "/" + maxPriorityMember.getPassword() + "/" + relPath + "/"
                  + fileNameArray[i] + "/" + fSize + "/" + ReplicationTestService.Constants.OperationType.ADD_NT_FILE;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(maxPriorityMember);
            String result = client.execute(url);
            System.out.println(url);
            System.out.println(result);

            assertEquals(result, "ok");
         }

         Thread.sleep(60 * 1000);

         // allow connect to minPriorityMember
         {
            String disconnectUrl =
               "http://" + minPriorityMember.getIpAddress() + ":" + minPriorityMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + minPriorityMember.getLogin() + "/" + minPriorityMember.getPassword() + "/"
                  + ReplicationTestService.Constants.OperationType.ALLOW_CONNECT;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(minPriorityMember);
            String result = client.execute(disconnectUrl);
            System.out.println(disconnectUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }

         // wait 4 minutes (reconnect + restore will be finished)
         Thread.sleep(4 * 60 * 1000);

         // check nt:file in members
         randomizeMembers();

         for (int i = 0; i < filesSize.length; i++)
         {
            long fSize = filesSize[i];
            String relPath = relPathArray[i];

            for (MemberInfo member : getAllMembers())
            {
               String checkUrl =
                  "http://" + member.getIpAddress() + ":" + member.getPort()
                     + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace
                     + "/" + member.getLogin() + "/" + member.getPassword() + "/" + relPath + "/" + fileNameArray[i]
                     + "/" + fSize + "/" + ReplicationTestService.Constants.OperationType.CHECK_NT_FILE;

               BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(member, 4000);
               String result = client.execute(checkUrl);
               System.out.println(checkUrl);
               System.out.println(result);

               assertEquals(result, "ok");
            }
         }
         System.out.println("End iteration #" + iteration);
      }
   }
}
