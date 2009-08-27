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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 13.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BackupJobConfig.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class BackupJobConfig
{
   /**
    * The FQN (Fully qualified name) to BackupJob class .
    */
   private String backupJob;

   /**
    * The parameters to BackupJob.
    */
   private Collection<Pair> parameters;

   /**
    * BackupJobConfig constructor. The empty constructor.
    */
   public BackupJobConfig()
   {
      backupJob = "";
      parameters = new ArrayList<Pair>();
   }

   /**
    * BackupJobConfig constructor.
    * 
    * @param backupJob
    *          String, the FQN (Fully qualified name) to BackupJob class .
    * @param parameters
    *          Collection, the list of parameters
    */
   public BackupJobConfig(String backupJob, Collection<Pair> parameters)
   {
      this.backupJob = backupJob;
      this.parameters = parameters;
   }

   /**
    * getBackupJob.
    * 
    * @return String return the FQN to BackupJob
    */
   public String getBackupJob()
   {
      return backupJob;
   }

   /**
    * setBackupJob.
    * 
    * @param backupJob
    *          String, the FQN to BackupJob
    */
   public void setBackupJob(String backupJob)
   {
      this.backupJob = backupJob;
   }

   /**
    * getParameters.
    * 
    * @return Collection the parameters to BAckupJob
    */
   public Collection<Pair> getParameters()
   {
      return parameters;
   }

   /**
    * setParameters.
    * 
    * @param parameters
    *          Collection, the parameters to BAckupJob
    */
   public void setParameters(Collection<Pair> parameters)
   {
      this.parameters = parameters;
   }

}
