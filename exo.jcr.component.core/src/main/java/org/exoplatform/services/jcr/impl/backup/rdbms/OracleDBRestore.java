/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.backup.rdbms;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleaner;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: OracleDBRestore.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class OracleDBRestore extends DBRestore
{

   /**
    *  OracleDBRestore constructor.
    */
   public OracleDBRestore(File storageDir, Connection jdbcConn, Map<String, RestoreTableRule> tables,
      WorkspaceEntry wsConfig, FileCleaner fileCleaner, DBCleaner dbCleaner) throws NamingException, SQLException,
      RepositoryConfigurationException
   {
      super(storageDir, jdbcConn, tables, wsConfig, fileCleaner, dbCleaner);
   }

   /**
    * {@inheritDoc}
    */
   protected void prepareQueries(boolean isMultiDb)
   {
      String multiDb = isMultiDb ? "M" : "S";

      String indexName = "JCR_IDX_" + multiDb + "ITEM_PARENT_FK";
      addQueries.put(indexName, "CREATE INDEX " + indexName + " ON JCR_" + multiDb + "ITEM(PARENT_ID)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);

      indexName = "JCR_IDX_" + multiDb + "ITEM_PARENT";
      addQueries.put(indexName, "CREATE UNIQUE INDEX " + indexName + " ON JCR_" + multiDb
         + "ITEM(CONTAINER_NAME, PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION DESC)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);

      indexName = "JCR_IDX_" + multiDb + "ITEM_PARENT_NAME";
      addQueries.put(indexName, "CREATE UNIQUE INDEX " + indexName + " ON JCR_" + multiDb
         + "ITEM(I_CLASS, CONTAINER_NAME, PARENT_ID, NAME, I_INDEX, VERSION DESC)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);

      indexName = "JCR_IDX_" + multiDb + "ITEM_PARENT_ID";
      addQueries.put(indexName, "CREATE UNIQUE INDEX " + indexName + " ON JCR_" + multiDb
         + "ITEM(I_CLASS, CONTAINER_NAME, PARENT_ID, ID, VERSION DESC)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);

      indexName = "JCR_IDX_" + multiDb + "VALUE_PROPERTY";
      addQueries.put(indexName, "CREATE UNIQUE INDEX " + indexName + " ON JCR_" + multiDb
         + "VALUE(PROPERTY_ID, ORDER_NUM)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);

      indexName = "JCR_IDX_" + multiDb + "REF_PROPERTY";
      addQueries.put(indexName, "CREATE UNIQUE INDEX " + indexName + " ON JCR_" + multiDb
         + "REF(PROPERTY_ID, ORDER_NUM)");
      dropQueries.put(indexName, "DROP INDEX " + indexName);
      
      super.prepareQueries(isMultiDb);
   }
}
