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

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BaseTestCaseChecker.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public abstract class BaseTestCaseChecker extends TestCase
{

   // for exo-application
   public final static String TEST_REALM = "eXo REST services";

   // for ECM
   // public final static String TEST_REALM = "exo-domain";

   protected static int MAX_RANDOM_VALUE = 1000000;

   protected final String workingRepository = "repository";

   protected final String workingWorkspace = "production";

   private final MemberInfo[] members =
      new MemberInfo[]{new MemberInfo("192.168.0.102", 8080, "root", "exo", 100),
         new MemberInfo("192.168.0.102", 8080, "root", "exo", 50),
         new MemberInfo("192.168.0.102", 8080, "root", "exo", 30)};

   private int maxPriorityMemberIndex;

   private int minPriorityMemberIndex;

   private MemberInfo masterMember;

   private MemberInfo[] slaveMembers;

   protected void setUp() throws Exception
   {
      // check max and min priority members;
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      int minIndex = -1, maxIndex = -1;

      for (int mIndex = 0; mIndex < members.length; mIndex++)
      {
         MemberInfo memberInfo = members[mIndex];

         if (min > memberInfo.getPriority())
         {
            min = memberInfo.getPriority();
            minIndex = mIndex;
         }

         if (max < memberInfo.getPriority())
         {
            max = memberInfo.getPriority();
            maxIndex = mIndex;
         }
      }

      minPriorityMemberIndex = minIndex;
      maxPriorityMemberIndex = maxIndex;
   }

   protected String createRelPath(long fSize)
   {
      String alphabet = "abcdefghijklmnopqrstuvwxyz";
      String relPath = "";
      long pathDepth = (fSize % 7) + 5;

      for (long i = 0; i < pathDepth; i++)
      {
         int index1 = (int)(Math.random() * 1000) % alphabet.length();
         int index2 = (int)(Math.random() * 1000) % alphabet.length();
         String s = alphabet.substring(index1, index1 + 1) + alphabet.substring(index2, index2 + 1);
         // s+=(int) (Math.random() * 100000);

         relPath += ("::" + s);
      }

      return relPath;
   }

   public MemberInfo getCurrentMasterMember()
   {
      return masterMember;
   }

   public MemberInfo getMaxPriorityMember()
   {
      return members[maxPriorityMemberIndex];
   }

   public MemberInfo getMinPriorityMember()
   {
      return members[minPriorityMemberIndex];
   }

   public MemberInfo getMiddlePriorityMember()
   {
      for (MemberInfo memberInfo : members)
         if (!memberInfo.equals(members[maxPriorityMemberIndex]) && !memberInfo.equals(members[minPriorityMemberIndex]))
         {
            return memberInfo;
         }
      return members[minPriorityMemberIndex];
   }

   public MemberInfo[] getCurrentSlaveMembers()
   {
      return slaveMembers;
   }

   public MemberInfo[] getAllMembers()
   {
      return members;
   }

   public void randomizeMembers()
   {
      int masterIndex = (int)(Math.random() * 1000) % members.length;

      masterMember = members[masterIndex];

      slaveMembers = new MemberInfo[members.length - 1];

      int slaveMembersIndex = 0;

      for (int i = 0; i < members.length; i++)
         if (i != masterIndex)
            slaveMembers[slaveMembersIndex++] = members[i];
   }

   public long getRandomLong()
   {
      return (long)(Math.random() * MAX_RANDOM_VALUE);
   }
}
