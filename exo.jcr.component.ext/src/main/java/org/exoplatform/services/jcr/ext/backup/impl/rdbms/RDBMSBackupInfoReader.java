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
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about performed backup.
 * 
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RDBMSBackupInfoReader.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RDBMSBackupInfoReader
{

   /**
    * File which contains necessary information about backup.
    */
   public static final String BACKUP_INFO = "backup.info";

   /**
    * Workspace name.
    */
   private final String workspaceName;

   /**
    * Repository name.
    */
   private final String repositoryName;

   /**
    * Is multi-db.
    */
   private final boolean isMultiDb;

   /**
    * Table name for items.
    */
   private final String itemTableName;

   /**
    * Table name for values.
    */
   private final String valueTableName;

   /**
    * Table name for referenceable data.
    */
   private final String refTableName;

   /**
    * Lock table names.
    */
   private final List<String> lockTableNames = new ArrayList<String>();

   /**
    * Constructor RDBMSBackupInfoReader.java.
    * 
    * @param dir
    *          The directory where file with backup information was stored.
    */
   public RDBMSBackupInfoReader(String dir) throws IOException
   {
      ObjectReader backupInfoReader =
         new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(new File(dir, BACKUP_INFO)));

      try
      {
         this.repositoryName = backupInfoReader.readString();
         this.workspaceName = backupInfoReader.readString();
         this.isMultiDb = backupInfoReader.readBoolean();
         this.itemTableName = backupInfoReader.readString();
         this.valueTableName = backupInfoReader.readString();
         this.refTableName = backupInfoReader.readString();

         int lockTablesCount = backupInfoReader.readInt();
         for (int i = 0; i < lockTablesCount; i++)
         {
            lockTableNames.add(backupInfoReader.readString());
         }
      }
      finally
      {
         backupInfoReader.close();
      }
   }

   /**
    * Returns the original repository name where backup was performed.
    * 
    * @return repository name
    */
   public String getRepositoryName()
   {
      return repositoryName;
   }

   /**
    * Returns the original workspace name where backup was performed.
    * 
    * @return workspace name
    */
   public String getWorkspaceName()
   {
      return workspaceName;
   }

   /**
    * Returns the original value of multi-db parameter of workspace from which backup was performed.
    * 
    * @return multi-db parameter 
    */
   public boolean isMultiDb()
   {
      return isMultiDb;
   }

   /**
    * Returns the original table name of items from which backup was performed.
    * 
    * @return table name
    */
   public String getItemTableName()
   {
      return itemTableName;
   }

   /**
    * Returns the original table name of values from which backup was performed.
    * 
    * @return table name
    */
   public String getValueTableName()
   {
      return valueTableName;
   }

   /**
    * Returns the original table name of referenceable data from which backup was performed.
    * 
    * @return table name
    */
   public String getRefTableName()
   {
      return refTableName;
   }

   /**
    * Returns the original table names of lock data from which backup was performed.
    * 
    * @return list of table names
    */
   public List<String> getLockTableNames()
   {
      return lockTableNames;
   }
}
