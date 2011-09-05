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
package org.exoplatform.services.jcr.ext.replication.storage;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: IncomeDataContext.java 111 2008-11-11 11:11:11Z serg $
 */
public class IncomeDataContext
{
   /**
    * The random changes file.
    */
   private final RandomChangesFile changesFile;

   /**
    * The description to mamber.
    */
   private final Member member;

   /**
    * The total packets.
    */
   private final long totalPackets;

   /**
    * The saved packets.
    */
   private long savedPackets;

   /**
    * Constructor.
    * 
    * @param changesFile
    *          file to store income changes;
    * @param member
    *          owner of income data;
    * @param totalPackets
    *          total packets count
    */
   public IncomeDataContext(RandomChangesFile changesFile, Member member, long totalPackets)
   {
      this.changesFile = changesFile;
      this.member = member;
      this.totalPackets = totalPackets;
      this.savedPackets = 0;
   }

   /**
    * getChangesFile.
    *
    * @return RandomChangesFile
    *           return the random changes file
    */
   public RandomChangesFile getChangesFile()
   {
      return changesFile;
   }

   /**
    * getMember.
    *
    * @return Member
    *           return the member 
    */
   public Member getMember()
   {
      return member;
   }

   /**
    * writeData.
    *
    * @param buf 
    *          byte[], piece of data
    * @param offset
    *          long, offset for data
    * @throws IOException
    *           will be generated piece of data
    */
   public void writeData(byte[] buf, long offset) throws IOException
   {
      changesFile.writeData(buf, offset);
      savedPackets++;
      if (savedPackets == totalPackets)
         changesFile.finishWrite();
   }

   /**
    * Check of all data saved.
    *
    * @return boolean
    *           return boolean value of all data saved 
    */
   public boolean isFinished()
   {
      return (savedPackets == totalPackets);
   }

}
