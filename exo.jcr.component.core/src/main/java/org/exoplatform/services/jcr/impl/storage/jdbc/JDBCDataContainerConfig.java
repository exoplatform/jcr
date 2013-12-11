/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.jdbc.DataSourceProvider;

/**
 * Contains JDBCWorkspaceDataContainer-specific configuration
 * 
 * Created by The eXo Platform SAS 1.03.2012. 
 * 
 * @author <a href="mailto:nzamosenchuk@exoplatform.com.ua">Nikolay Zamosenchuk</a>
 * @version $Id: JDBCDataContainerConfig.java 5024 2012-03-01 13:36:58Z nzasemochuk $
 */
public class JDBCDataContainerConfig
{
   public static enum DatabaseStructureType {
      /**
       * Each workspace has it's own database
       */
      MULTI {
         @Override
         public boolean isMultiDatabase()
         {
            return true;
         }

         @Override
         public boolean isShareSameDatasource()
         {
            return false;
         }
      },
      /**
       * All workspaces from each repositories can be stored in a single database within one set of 
       * tables.
       */
      SINGLE {
         @Override
         public boolean isMultiDatabase()
         {
            return false;
         }

         @Override
         public boolean isShareSameDatasource()
         {
            return true;
         }
      },
      /**
       * "ISOLATED" database structure combines SINGLE and MULTI, by offering workspace and repository 
       * storage isolation within one datasource using per-workspace table spaces. So each workspace 
       * have it's own set of database tables, that are isolated from others.  
       */
      ISOLATED {
         @Override
         public boolean isMultiDatabase()
         {
            return true;
         }

         @Override
         public boolean isShareSameDatasource()
         {
            return true;
         }
      };

      /**
       * @return true if every workspace stores data in separate table within one repository
       */
      public abstract boolean isMultiDatabase();

      /**
       * @return true if every workspace should have same datasource name parameter in container
       */
      public abstract boolean isShareSameDatasource();
   }

   /**
    * Use additional connection to database to check same name siblings
    */
   public boolean checkSNSNewConnection;

   /**
    * Container name
    */
   public String containerName;

   /**
    * Database dialect
    */
   public String dbDialect;

   /**
    * Datasource name
    */
   public String dbSourceName;

   /**
    * Data structure type, replaces deprecated multiDb
    */
   public JDBCDataContainerConfig.DatabaseStructureType dbStructureType;

   /**
    * Suffix used in tables names when isolated-databse structure used 
    */
   public String dbTableSuffix;

   /**
    * Datasource provider
    */
   public DataSourceProvider dsProvider;

   /**
    * If datasource is managed by outer container
    */
   public boolean isManaged;

   /**
    * Version of persisted storage
    */
   public String storageVersion;

   /**
    * Spool container configuration.
    */
   public SpoolConfig spoolConfig;

   /**
    * Unique container name
    */
   public String uniqueName;

   /**
    * Some DataBases supports query hints, that may improve query performance.
    * For default hints are enabled.
    */
   public boolean useQueryHints;

   /**
    * ValueStorage provier
    */
   public ValueStoragePluginProvider valueStorageProvider;

   /**
    * Path to SQL initialization scripts
    */
   public String initScriptPath;

   /**
    * Batch size.
    */
   public int batchSize;

   /**
    * Use sequence for order number.
    */
   public boolean useSequenceForOrderNumber;

}