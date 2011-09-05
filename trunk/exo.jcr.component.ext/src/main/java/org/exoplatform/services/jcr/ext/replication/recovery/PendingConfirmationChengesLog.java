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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PendingConfirmationChengesLog.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class PendingConfirmationChengesLog
{
   /**
    * The list of members who has been saved successfully.
    */
   private List<String> confirmationList;

   /**
    * The list of members who has not been saved successfully.
    */
   private List<String> notConfirmationList;

   /**
    * Pending the ChangesLog.
    */
   private ItemStateChangesLog changesLog;

   /**
    * The date of save the ChchgesLog.
    */
   private Calendar timeStamp;

   /**
    * The identification string to PendingConfirmationChengesLog.
    */
   private String identifier;

   /**
    * The path to data file.
    */
   private String dataFilePath;

   /**
    * PendingConfirmationChengesLog constructor.
    * 
    * @param changesLog
    *          the ChangesLog with data
    * @param timeStamp
    *          the save date
    * @param identifier
    *          the identifier string
    */
   public PendingConfirmationChengesLog(ItemStateChangesLog changesLog, Calendar timeStamp, String identifier)
   {
      this.confirmationList = new ArrayList<String>();
      this.changesLog = changesLog;
      this.timeStamp = timeStamp;
      this.identifier = identifier;
   }

   /**
    * getConfirmationList.
    * 
    * @return List return the list of members who has not been saved successfully
    */
   public List<String> getConfirmationList()
   {
      return confirmationList;
   }

   /**
    * setConfirmationList.
    * 
    * @param confirmationList
    *          the list of members who has been saved successfully
    */
   public void setConfirmationList(List<String> confirmationList)
   {
      this.confirmationList = confirmationList;
   }

   /**
    * getChangesLog.
    * 
    * @return ItemStateChangesLog return the ChangesLog
    */
   public ItemStateChangesLog getChangesLog()
   {
      return changesLog;
   }

   /**
    * setChangesLog.
    * 
    * @param changesLog
    *          the ChangesLog
    */
   public void setChangesLog(ItemStateChangesLog changesLog)
   {
      this.changesLog = changesLog;
   }

   /**
    * getTimeStamp.
    * 
    * @return Calendar return the date of ChangesLog
    */
   public Calendar getTimeStamp()
   {
      return timeStamp;
   }

   /**
    * setTimeStamp.
    * 
    * @param timeStamp
    *          the Calendar
    */
   public void setTimeStamp(Calendar timeStamp)
   {
      this.timeStamp = timeStamp;
   }

   /**
    * getIdentifier.
    * 
    * @return String return the identification string
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * setIdentifier.
    * 
    * @param identifier
    *          the identification string
    */
   public void setIdentifier(String identifier)
   {
      this.identifier = identifier;
   }

   /**
    * getNotConfirmationList.
    * 
    * @return List return the list of members who has not been saved successfully
    */
   public List<String> getNotConfirmationList()
   {
      return notConfirmationList;
   }

   /**
    * setNotConfirmationList.
    * 
    * @param notConfirmationList
    *          the list of members who has not been saved successfully
    */
   public void setNotConfirmationList(List<String> notConfirmationList)
   {
      this.notConfirmationList = notConfirmationList;
   }

   /**
    * getDataFilePath.
    *
    * @return String
    *           the path to data file.
    */
   public String getDataFilePath()
   {
      return dataFilePath;
   }

   /**
    * setDataFilePath.
    *
    * @param dataFilePath
    *          String, path to data file.
    */
   public void setDataFilePath(String dataFilePath)
   {
      this.dataFilePath = dataFilePath;
   }

}