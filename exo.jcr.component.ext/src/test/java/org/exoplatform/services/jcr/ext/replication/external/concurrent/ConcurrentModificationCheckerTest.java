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
package org.exoplatform.services.jcr.ext.replication.external.concurrent;

import org.exoplatform.services.jcr.ext.replication.external.BaseTestCaseChecker;
import org.exoplatform.services.jcr.ext.replication.external.BasicAuthenticationHttpClient;
import org.exoplatform.services.jcr.ext.replication.external.MemberInfo;
import org.exoplatform.services.jcr.ext.replication.test.ReplicationTestService;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ConcurrentModificationCheckerTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ConcurrentModificationCheckerTest extends BaseTestCaseChecker
{

   public void testTwoThreads() throws Exception
   {
      String relPath[] =
         new String[]{createRelPath(getRandomLong()), createRelPath(getRandomLong()), createRelPath(getRandomLong())};

      String fileName[] =
         new String[]{"nt_file_" + getRandomLong(), "nt_file_" + getRandomLong(), "nt_file_" + getRandomLong()};

      String simpleContent[] = new String[]{"jim_kfhcpw", "kemlko_ekq", "qkhf_ss"};

      long iterations[] = new long[]{21565, 27048, 57562};

      long updateIterations = 1500;

      String srcRelPath1 = relPath[0];
      String srcRelPath2 = relPath[1];
      String destRelPath = relPath[2];

      String srcFileName1 = fileName[0];
      String srcFileName2 = fileName[1];
      String destFileName = fileName[2];

      // create two nt:file of base content to masterMember
      randomizeMembers();

      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < relPath.length; i++)
      {
         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + relPath[i] + "/" + fileName[i]
               + "/" + iterations[i] + "/" + simpleContent[i] + "/"
               + ReplicationTestService.Constants.OperationType.CREATE_CONTENT;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      Thread.sleep(5000);

      // start two threads for update the binary value
      for (int i = 0; i < 2; i++)
      {
         {
            String url =
               "http://" + getMaxPriorityMember().getIpAddress() + ":" + getMaxPriorityMember().getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + getMaxPriorityMember().getLogin() + "/" + getMaxPriorityMember().getPassword() + "/" + srcRelPath1
                  + "/" + srcFileName1 + "/" + destRelPath + "/" + destFileName + "/" + updateIterations + "/"
                  + ReplicationTestService.Constants.OperationType.START_THREAD_UPDATER;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMaxPriorityMember());
            String result = client.execute(url);
            System.out.println(url);
            System.out.println(result);

            assertEquals(result, "ok");
         }

         {
            String url =
               "http://" + getMinPriorityMember().getIpAddress() + ":" + getMinPriorityMember().getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + getMinPriorityMember().getLogin() + "/" + getMinPriorityMember().getPassword() + "/" + srcRelPath2
                  + "/" + srcFileName2 + "/" + destRelPath + "/" + destFileName + "/" + updateIterations + "/"
                  + ReplicationTestService.Constants.OperationType.START_THREAD_UPDATER;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
            String result = client.execute(url);
            System.out.println(url);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }

      // wait 4 minutes (update thread will be finished)
      Thread.sleep(10 * 60 * 1000);

      // check content
      for (MemberInfo memberInfo : getAllMembers())
      {
         String result1, result2;

         // compare with srcRelPath2
         {
            String url =
               "http://" + memberInfo.getIpAddress() + ":" + memberInfo.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + memberInfo.getLogin() + "/" + memberInfo.getPassword() + "/" + srcRelPath2 + "/" + srcFileName2
                  + "/" + destRelPath + "/" + destFileName + "/"
                  + ReplicationTestService.Constants.OperationType.COMPARE_DATA;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(getMinPriorityMember());
            result2 = client.execute(url);
            System.out.println(url);
            System.out.println(result2);
         }

         // compare with srcRelPath1
         {
            String url =
               "http://" + memberInfo.getIpAddress() + ":" + memberInfo.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + memberInfo.getLogin() + "/" + memberInfo.getPassword() + "/" + srcRelPath1 + "/" + srcFileName1
                  + "/" + destRelPath + "/" + destFileName + "/"
                  + ReplicationTestService.Constants.OperationType.COMPARE_DATA;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(memberInfo);
            result1 = client.execute(url);
            System.out.println(url);
            System.out.println(result1);
         }

         assertTrue("ok".equals(result1) || "ok".equals(result2));
      }
   }
}
