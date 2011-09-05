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
package org.exoplatform.services.jcr.ext.backup.server.bean.response;

import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.server.HTTPBackupAgent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 13.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ShortInfo.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ShortInfo
{

   /**
    * The type for current backup.
    */
   public static final int CURRENT = -1;

   /**
    * The type for completed backup.
    */
   public static final int COMPLETED = 0;

   /**
    * The type for current restore.
    */
   public static final int RESTORE = 1;

   /**
    * The type of ShortInfo (current, completed, restore).
    */
   private Integer type;

   /**
    * The backup identifier.
    */
   private String backupId;

   /**
    * The backup type (full or full+incremental).
    */
   private Integer backupType;

   /**
    * The repository name.
    */
   private String repositoryName;

   /**
    * The workspace name.
    */
   private String workspaceName;

   /**
    * The state of backup or restore.
    */
   private Integer state;

   /**
    * The started time of backup or restore. The date in format RFC 1123.
    */
   private String startedTime;

   /**
    * The finished time of backup or restore. The date in format RFC 1123.
    */
   private String finishedTime;

   /**
    * ShortInfo.
    * 
    * Empty constructor.
    * 
    */
   public ShortInfo()
   {
   }

   /**
    * ShortInfo constructor.
    * 
    * @param type
    *          int, the tupe of short info
    * @param chain
    *          BackupChain, the backup chain for current backup.
    */
   public ShortInfo(int type, BackupChain chain)
   {
      this.type = type;
      this.backupType = chain.getBackupConfig().getBackupType();
      this.backupId = chain.getBackupId();
      this.repositoryName = chain.getBackupConfig().getRepository();
      this.workspaceName = chain.getBackupConfig().getWorkspace();
      this.state = chain.getFullBackupState();

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(chain.getStartedTime().getTime());

      // no applicable
      this.finishedTime = "";
   }

   /**
    * ShortInfo constructor.
    * 
    * @param type
    *          int, the tupe of short info
    * @param chain
    *          RepositoryBackupChain, the backup chain for current backup.
    */
   public ShortInfo(int type, RepositoryBackupChain chain)
   {
      this.type = type;
      this.backupType = chain.getBackupConfig().getBackupType();
      this.backupId = chain.getBackupId();
      this.repositoryName = chain.getBackupConfig().getRepository();
      this.state = chain.getState();

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(chain.getStartedTime().getTime());

      // no applicable
      this.finishedTime = "";
      this.workspaceName = "";
   }

   /**
    * ShortInfo constructor.
    * 
    * @param type
    *          int, the tupe of short info
    * @param chainLog
    *          BackupChainLog, the backup chain log for completed backup.
    */
   public ShortInfo(int type, BackupChainLog chainLog)
   {
      this.type = type;
      this.backupType = chainLog.getBackupConfig().getBackupType();
      this.backupId = chainLog.getBackupId();
      this.repositoryName = chainLog.getBackupConfig().getRepository();
      this.workspaceName = chainLog.getBackupConfig().getWorkspace();

      // do not use
      this.state = 0;

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(chainLog.getStartedTime().getTime());
      this.finishedTime = df.format(chainLog.getFinishedTime().getTime());;
   }

   /**
    * ShortInfo constructor.
    * 
    * @param type
    *          int, the tupe of short info
    * @param chainLog
    *          BackupChainLog, the backup chain log for completed backup.
    */
   public ShortInfo(int type, RepositoryBackupChainLog chainLog)
   {
      this.type = type;
      this.backupType = chainLog.getBackupConfig().getBackupType();
      this.backupId = chainLog.getBackupId();
      this.repositoryName = chainLog.getBackupConfig().getRepository();
      this.workspaceName = "";

      // do not use
      this.state = 0;

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(chainLog.getStartedTime().getTime());
      this.finishedTime = df.format(chainLog.getFinishedTime().getTime());;
   }

   /**
    * ShortInfo constructor.
    * 
    * For restore.
    * 
    * @param type
    *          int, the tupe of short info
    * @param chainLog
    *          BackupChainLog, the backup chain log for completed backup.
    * @param startedTime
    *          Calendar, the stated time
    * @param finishedTime
    *          Calendar, the finished time
    * @param state
    *          int, the state of restore
    */
   public ShortInfo(int type, BackupChainLog chainLog, Calendar startedTime, Calendar finishedTime, int state)
   {
      this.type = type;
      this.backupType = chainLog.getBackupConfig().getBackupType();
      this.backupId = chainLog.getBackupId();
      this.repositoryName = chainLog.getBackupConfig().getRepository();
      this.workspaceName = chainLog.getBackupConfig().getWorkspace();

      this.state = state;

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(startedTime.getTime());

      if (finishedTime != null)
         this.finishedTime = df.format(finishedTime.getTime());
      else
         this.finishedTime = "";
   }

   /**
    * ShortInfo constructor.
    * 
    * For restore.
    * 
    * @param type
    *          int, the type of short info
    * @param chainLog
    *          RepositoryBackupChainLog, the backup chain log for completed backup.
    * @param startedTime
    *          Calendar, the stated time
    * @param finishedTime
    *          Calendar, the finished time
    * @param state
    *          int, the state of restore
    */
   public ShortInfo(int type, RepositoryBackupChainLog chainLog, Calendar startedTime, Calendar finishedTime, int state)
   {
      this.type = type;
      this.backupType = chainLog.getBackupConfig().getBackupType();
      this.backupId = chainLog.getBackupId();
      this.repositoryName = chainLog.getBackupConfig().getRepository();

      this.state = state;

      DateFormat df = new SimpleDateFormat(HTTPBackupAgent.Constants.DATE_FORMAT_RFC_1123);
      this.startedTime = df.format(startedTime.getTime());

      if (finishedTime != null)
         this.finishedTime = df.format(finishedTime.getTime());
      else
         this.finishedTime = "";
   }

   /**
    * ShortInfo constructor.
    * 
    * For restore.
    *
    * @param type
    *          int, the tupe of short info
    * @param chainLog
    *          BackupChainLog, the backup chain log for completed backup. 
    * @param startedTime
    *          Calendar, the stated time
    * @param finishedTime
    *          Calendar, the finished time
    * @param state
    *          int, the state of restore
    * @param repositroryName
    *          String, the repository name
    * @param workspaceName
    *          String, the workspace name           
    */
   public ShortInfo(int type, BackupChainLog chainLog, Calendar startedTime, Calendar finishedTime, int state,
      String repositroryName, String workspaceName)
   {
      this(type, chainLog, startedTime, finishedTime, state);
      this.repositoryName = repositroryName;
      this.workspaceName = workspaceName;
   }

   /**
    * ShortInfo constructor.
    * 
    * For restore.
    *
    * @param type
    *          int, the tupe of short info
    * @param chainLog
    *          BackupChainLog, the backup chain log for completed backup. 
    * @param startedTime
    *          Calendar, the stated time
    * @param finishedTime
    *          Calendar, the finished time
    * @param state
    *          int, the state of restore
    * @param repositroryName
    *          String, the repository name
    */
   public ShortInfo(int type, RepositoryBackupChainLog chainLog, Calendar startedTime, Calendar finishedTime,
      int state, String repositroryName)
   {
      this(type, chainLog, startedTime, finishedTime, state);
      this.repositoryName = repositroryName;

      // no applicable
      this.workspaceName = "";
   }

   /**
    * getState.
    * 
    * @return Integer the state of backup or restore
    */
   public Integer getState()
   {
      return state;
   }

   /**
    * setState.
    * 
    * @param state
    *          Integer, the state of backup or restore
    */
   public void setState(Integer state)
   {
      this.state = state;
   }

   /**
    * getBackupId.
    * 
    * @return String return the backup identifier
    */
   public String getBackupId()
   {
      return backupId;
   }

   /**
    * setBackupId.
    * 
    * @param backupId
    *          the backup identifier
    */
   public void setBackupId(String backupId)
   {
      this.backupId = backupId;
   }

   /**
    * getRepositoryName.
    * 
    * @return String return the repository name
    */
   public String getRepositoryName()
   {
      return repositoryName;
   }

   /**
    * setRepositoryName.
    * 
    * @param repositoryName
    *          String, repository name
    */
   public void setRepositoryName(String repositoryName)
   {
      this.repositoryName = repositoryName;
   }

   /**
    * getWorkspaceName.
    * 
    * @return String return the workspace name
    */
   public String getWorkspaceName()
   {
      return workspaceName;
   }

   /**
    * setWorkspaceName.
    * 
    * @param workspaceName
    *          String, the workspace name
    */
   public void setWorkspaceName(String workspaceName)
   {
      this.workspaceName = workspaceName;
   }

   /**
    * getStartedTime.
    * 
    * @return String return the started time of backup or restore
    */
   public String getStartedTime()
   {
      return startedTime;
   }

   /**
    * setStartedTime.
    * 
    * @param startedTime
    *          String, the started time of backup or restore
    */
   public void setStartedTime(String startedTime)
   {
      this.startedTime = startedTime;
   }

   /**
    * getFinishedTime.
    * 
    * @return String return the finished time of backup or restore
    */
   public String getFinishedTime()
   {
      return finishedTime;
   }

   /**
    * setFinishedTime.
    * 
    * @param finishedTime
    *          String, the finished time of backup or restore
    */
   public void setFinishedTime(String finishedTime)
   {
      this.finishedTime = finishedTime;
   }

   /**
    * getType.
    * 
    * @return Integer return the type of ShortInfo
    */
   public Integer getType()
   {
      return type;
   }

   /**
    * setType.
    * 
    * @param type
    *          Integer, the type of ShortInfo
    */
   public void setType(Integer type)
   {
      this.type = type;
   }

   /**
    * getBackupType.
    * 
    * @return Integer return the backup type
    */
   public Integer getBackupType()
   {
      return backupType;
   }

   /**
    * setBackupType.
    * 
    * @param backupType
    *          Integer, the backup type
    */
   public void setBackupType(Integer backupType)
   {
      this.backupType = backupType;
   }
}
