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
 * @version $Id: LockCheckerTest.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class LockCheckerTest extends BaseTestCaseChecker
{
   public void testLock() throws Exception
   {
      int MANY_TEST = 2;
      String relPathArray[] = new String[MANY_TEST];

      // set lock to masterMember
      randomizeMembers();
      MemberInfo masterMember = getCurrentMasterMember();

      for (int i = 0; i < relPathArray.length; i++)
      {
         int rendomValue = (int)(Math.random() * MAX_RANDOM_VALUE);
         String relPath = createRelPath(rendomValue) + "::" + "locked_node" + rendomValue;
         relPathArray[i] = relPath;

         String url =
            "http://" + masterMember.getIpAddress() + ":" + masterMember.getPort()
               + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
               + masterMember.getLogin() + "/" + masterMember.getPassword() + "/" + relPath + "/"
               + ReplicationTestService.Constants.OperationType.SET_LOCK;

         BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(masterMember, 500);
         String result = client.execute(url);
         System.out.println(url);
         System.out.println(result);

         assertEquals(result, "ok");
      }

      // check lock in slaveMember

      for (int i = 0; i < relPathArray.length; i++)
      {
         String relPath = relPathArray[i];

         for (MemberInfo slaveMember : getCurrentSlaveMembers())
         {
            String checkUrl =
               "http://" + slaveMember.getIpAddress() + ":" + slaveMember.getPort()
                  + ReplicationTestService.Constants.BASE_URL + "/" + workingRepository + "/" + workingWorkspace + "/"
                  + slaveMember.getLogin() + "/" + slaveMember.getPassword() + "/" + relPath + "/"
                  + ReplicationTestService.Constants.OperationType.CECK_LOCK;

            BasicAuthenticationHttpClient client = new BasicAuthenticationHttpClient(slaveMember, 500);
            String result = client.execute(checkUrl);
            System.out.println(checkUrl);
            System.out.println(result);

            assertEquals(result, "ok");
         }
      }
   }
}
