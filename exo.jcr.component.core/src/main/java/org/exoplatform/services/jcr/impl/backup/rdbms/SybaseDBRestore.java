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

import org.exoplatform.services.database.utils.ExceptionManagementHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: SybaseDBRestore.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class SybaseDBRestore
   extends DBRestore
{
   private String restoreConstraint = null;
   
   public SybaseDBRestore(File storageDir, Connection jdbcConn, Map<String, RestoreTableRule> tables,
            WorkspaceEntry wsConfig, FileCleaner fileCleaner) throws NamingException, SQLException,
            RepositoryConfigurationException
   {
      super(storageDir, jdbcConn, tables, wsConfig, fileCleaner);
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {

      try
      {
         // the Sybase is not allowed DDL query (CREATE TABLE, DROP TABLE, etc. ) within a multi-statement transaction
         jdbcConn.setAutoCommit(true);

         for (Entry<String, RestoreTableRule> entry : tables.entrySet())
         {
            String tableName = entry.getKey();
            RestoreTableRule restoreRule = entry.getValue();

            super.preRestoreTable(tableName, restoreRule);
         }
      }
      catch (SQLException e)
      {
         throw new BackupException(ExceptionManagementHelper.getFullSQLExceptionMessage(e), e);
      }
      finally
      {
         try
         {
            jdbcConn.setAutoCommit(false);
         }
         catch (SQLException e)
         {
            LOG.warn("Can't set auto commit to \"false\"", e);
         }
      }

      super.clean();
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws BackupException
   {
      super.commit();
      
      restoreConstraint();
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws BackupException
   {
      BackupException rollbackException = null;

      try
      {
         super.rollback();
      }
      catch (BackupException e)
      {
         rollbackException = e;
         throw rollbackException;
      }
      finally
      {
         try
         {
            restoreConstraint();
         }
         catch (BackupException e)
         {
            if (rollbackException != null)
            {
               LOG.error("Can not restore constraint", e);
               throw rollbackException;
            }
            else
            {
               throw e;
            }
         }
      }
   }

   /**
    * Restore constraint.
    * 
    * @throws BackupException
    *           Will throw BackupException if fail.
    */
   private void restoreConstraint() throws BackupException
   {
      try
      {
         // restore constraint
         jdbcConn.setAutoCommit(true);

         for (Entry<String, RestoreTableRule> entry : tables.entrySet())
         {
            String tableName = entry.getKey();
            RestoreTableRule restoreRule = entry.getValue();

            super.postRestoreTable(tableName, restoreRule);
         }
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         try
         {
            jdbcConn.setAutoCommit(false);
         }
         catch (SQLException e)
         {
            LOG.warn("Can't set auto commit to \"false\"", e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void preRestoreTable(String tableName, RestoreTableRule restoreRule) throws SQLException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void postRestoreTable(String tableName, RestoreTableRule restoreRule) throws SQLException
   {
   }
}
