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
package org.exoplatform.services.jcr.ext.backup.impl.rdbms;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about performed backup.
 * 
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RDBMSBackupInfo.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RDBMSBackupInfoWriter
{

   /**
    * Workspace name.
    */
   private String workspaceName;

   /**
    * Repository name.
    */
   private String repositoryName;

   /**
    * Is multi-db.
    */
   private boolean isMultiDb;

   /**
    * Table name for items.
    */
   private String itemTableName;

   /**
    * Table name for values.
    */
   private String valueTableName;

   /**
    * Table name for referenceable data.
    */
   private String refTableName;

   /**
    * Lock table names.
    */
   private List<String> lockTableNames = new ArrayList<String>();

   /**
    * The directory where file with backup information will be stored.
    */
   private final String dir;

   /**
    * Constructor RDBMSBackupInfoWriter.
    * 
    * @param dir
    *          The directory where file with backup information was stored.
    */
   public RDBMSBackupInfoWriter(String dir)
   {
      this.dir = dir;
   }

   /**
    * Returns the original repository name where backup was performed.
    * 
    * @return repository name
    */
   public void setRepositoryName(String repositoryName)
   {
      this.repositoryName = repositoryName;
   }

   /**
    * Returns the original workspace name where backup was performed.
    * 
    * @return workspace name
    */
   public void setWorkspaceName(String workspaceName)
   {
      this.workspaceName = workspaceName;
   }

   /**
    * Returns the original value of multi-db parameter of workspace from which backup was performed.
    * 
    * @return multi-db parameter 
    */
   public void setMultiDb(boolean isMultiDb)
   {
      this.isMultiDb = isMultiDb;
   }

   /**
    * Returns the original table name of items from which backup was performed.
    * 
    * @return table name
    */
   public void setItemTableName(String itemTableName)
   {
      this.itemTableName = itemTableName;
   }

   /**
    * Returns the original table name of values from which backup was performed.
    * 
    * @return table name
    */
   public void setValueTableName(String valueTableName)
   {
      this.valueTableName = valueTableName;
   }

   /**
    * Returns the original table name of referenceable data from which backup was performed.
    * 
    * @return table name
    */
   public void setRefTableName(String refTableName)
   {
      this.refTableName = refTableName;
   }

   /**
    * Returns the original table names of lock data from which backup was performed.
    * 
    * @return list of table names
    */
   public void setLockTableNames(List<String> lockTableNames)
   {
      this.lockTableNames.clear();
      this.lockTableNames.addAll(lockTableNames);
   }
   
   /**
    * Write backup information into file.
    * 
    * @throws IOException
    *          if any error occurred
    */
   public void write() throws IOException
   {
      ObjectWriter backupInfoWriter =
         new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(new File(dir, RDBMSBackupInfoReader.BACKUP_INFO)));

      try
      {
         backupInfoWriter.writeString(repositoryName);
         backupInfoWriter.writeString(workspaceName);
         backupInfoWriter.writeBoolean(isMultiDb);
         backupInfoWriter.writeString(itemTableName);
         backupInfoWriter.writeString(valueTableName);
         backupInfoWriter.writeString(refTableName);

         backupInfoWriter.writeInt(lockTableNames.size());
         for (int i = 0; i < lockTableNames.size(); i++)
         {
            backupInfoWriter.writeString(lockTableNames.get(i));
         }
      }
      finally
      {
         backupInfoWriter.close();
      }
   }
}
