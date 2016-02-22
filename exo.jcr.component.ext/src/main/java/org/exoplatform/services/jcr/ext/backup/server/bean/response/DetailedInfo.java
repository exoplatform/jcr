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
import org.exoplatform.services.jcr.ext.backup.server.bean.BackupConfigBean;

import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 13.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: DetailedInfo.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class DetailedInfo extends ShortInfo
{

   /**
    * The bean to backup config.
    */
   private BackupConfigBean backupConfig;

   /**
    * DetailedInfo constructor.
    * 
    */
   public DetailedInfo()
   {
   }

   /**
    * DetailedInfo constructor.
    * 
    * @param type
    *          int, the type of detailed info (current or completed)
    * @param chain
    *          backupChain
    */
   public DetailedInfo(int type, BackupChain chain)
   {
      super(type, chain);
      this.backupConfig = new BackupConfigBean(chain.getBackupConfig());
   }
   
   /**
    * DetailedInfo constructor.
    * 
    * @param type
    *          int, the type of detailed info (current or completed)
    * @param chain
    *          backupChain
    */
   public DetailedInfo(int type, RepositoryBackupChain chain)
   {
      super(type, chain);
      this.backupConfig = new BackupConfigBean(chain.getBackupConfig());
   }

   /**
    * DetailedInfo constructor.
    * 
    * @param type
    *          int, the type of detailed info (current or completed)
    * @param chainLog
    *          BackupChainLog
    */
   public DetailedInfo(int type, BackupChainLog chainLog)
   {
      super(type, chainLog);
      this.backupConfig = new BackupConfigBean(chainLog.getBackupConfig());
   }
   
   /**
    * DetailedInfo constructor.
    * 
    * @param type
    *          int, the type of detailed info (current or completed)
    * @param chainLog
    *          RepositoryBackupChainLog
    */
   public DetailedInfo(int type, RepositoryBackupChainLog chainLog)
   {
      super(type, chainLog);
      this.backupConfig = new BackupConfigBean(chainLog.getBackupConfig());
   }

   /**
    * DetailedInfo constructor.
    * 
    * For current restore.
    * 
    * @param type
    *          int, the type of DetailedInfo
    * @param chainLog
    *          BackupChainLog, chain log for restore
    * @param startedTime
    *          Calendar, the started time of restore
    * @param finishedTime
    *          Calendar, the finished time of restore
    * @param state
    *          int, the state of restore
    */
   public DetailedInfo(int type, BackupChainLog chainLog, Calendar startedTime, Calendar finishedTime, int state)
   {
      super(type, chainLog, startedTime, finishedTime, state);
      this.backupConfig = new BackupConfigBean(chainLog.getBackupConfig());
   }

   /**
    * DetailedInfo constructor.
    * 
    * For restore.
    *
    * @param type
    *          int, the type of DetailedInfo 
    * @param chainLog
    *          BackupChainLog, chain log for restore
    * @param startedTime
    *          Calendar, the started time of restore 
    * @param finishedTime
    *          Calendar, the finished time of restore
    * @param state
    *          int, the state of restore
    * @param repositroryName
    *          String, the repository name
    * @param workspaceName
    *          String, the workspace name           
    */
   public DetailedInfo(int type, BackupChainLog chainLog, Calendar startedTime, Calendar finishedTime, int state,
      String repositroryName, String workspaceName)
   {
      super(type, chainLog, startedTime, finishedTime, state, repositroryName, workspaceName);
      this.backupConfig = new BackupConfigBean(chainLog.getBackupConfig());
   }

   /**
    * DetailedInfo constructor.
    * 
    * For restore.
    *
    * @param type
    *          int, the type of DetailedInfo 
    * @param chainLog
    *          RepositoryBackupChainLog, chain log for restore
    * @param startedTime
    *          Calendar, the started time of restore 
    * @param finishedTime
    *          Calendar, the finished time of restore
    * @param state
    *          int, the state of restore
    * @param repositroryName
    *          String, the repository name
    */
   public DetailedInfo(int type, RepositoryBackupChainLog chainLog, Calendar startedTime, Calendar finishedTime,
      int state, String repositroryName)
   {
      super(type, chainLog, startedTime, finishedTime, state, repositroryName);
      this.backupConfig = new BackupConfigBean(chainLog.getBackupConfig());
   }

   /**
    * getBackupConfig.
    * 
    * @return BackupConfigBean return the bean to backup config
    */
   public BackupConfigBean getBackupConfig()
   {
      return backupConfig;
   }

   /**
    * setBackupConfig.
    * 
    * @param backupConfig
    *          BackupConfigBean, the backup config bean
    */
   public void setBackupConfig(BackupConfigBean backupConfig)
   {
      this.backupConfig = backupConfig;
   }

}
