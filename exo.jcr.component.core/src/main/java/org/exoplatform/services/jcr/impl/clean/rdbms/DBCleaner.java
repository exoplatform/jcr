/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.clean.rdbms;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The goal of this class is removing workspace data from database.
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleaner.java 3769 2011-01-04 15:36:06Z areshetnyak $
 */
public class DBCleaner
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBClean");

   /**
    * Connection to database.
    */
   protected final Connection connection;

   /**
    * Common clean scripts for database.
    */
   protected final List<String> cleanScripts = new ArrayList<String>();

   /**
    * Rollback scripts for database.
    */
   protected final List<String> rollbackScripts = new ArrayList<String>();

   /**
    * Commit scripts for database.
    */
   protected final List<String> commitScripts = new ArrayList<String>();

   /**
    * DB clean helper.
    */
   protected final DBCleanHelper dbCleanHelper;

   /**
    * Idicates if executing scripts should be done in autoCommit mode.                  
    */
   protected final boolean autoCommit;

   /**
    * DBCleaner constructor.
    * 
    * @param connection 
    *          connection to database where workspace tables is placed
    * @param cleanScripts
    *          scripts for cleaning database
    * @param rollbackScripts
    *          scripts for execution when something failed         
    * @param commitScripts
    *          scripts for removing temporary objects         
    * @param dbCleanHelper
    *          class which help to clean database by executing special queries
    * @param autoCommit
    *          indicates if executing scripts should be done in autoCommit mode                  
    */
   public DBCleaner(Connection connection, List<String> cleanScripts, List<String> rollbackScripts,
      List<String> commitScripts, DBCleanHelper dbCleanHelper, boolean autoCommit)
   {
      this.connection = connection;
      this.cleanScripts.addAll(cleanScripts);
      this.rollbackScripts.addAll(rollbackScripts);
      this.commitScripts.addAll(commitScripts);
      this.dbCleanHelper = dbCleanHelper;
      this.autoCommit = autoCommit;
   }

   /**
    * DBCleaner constructor.
    * 
    * @param connection 
    *          connection to database where workspace tables is placed
    * @param cleanScripts
    *          scripts for cleaning database
    * @param rollbackScripts
    *          scripts for execution when something failed         
    * @param commitScripts
    *          scripts for removing temporary objects
    * @param autoCommit
    *          indicates if executing scripts should be done in autoCommit mode                  
    */
   public DBCleaner(Connection connection, List<String> cleanScripts, List<String> rollbackScripts,
      List<String> commitScripts, boolean autoCommit)
   {
      this(connection, cleanScripts, rollbackScripts, commitScripts, null, autoCommit);
   }

   /**
    * Clean data from database. The method doesn't close connection or perform commit.
    * 
    * @throws SQLException
    *          if any errors occurred 
    */
   public void executeCleanScripts() throws SQLException
   {
      executeScripts(cleanScripts);

      if (dbCleanHelper != null)
      {
         dbCleanHelper.executeCleanScripts();
      }
   }

   /** 
    * Rollback changes. The method doesn't close connection or perform commit.
    *
    * @throws SQLException
    *          if any errors occurred 
    */
   public void executeRollbackScripts() throws SQLException
   {
      executeScripts(rollbackScripts);
   }

   /**
    * Cleaning temporary objects. The method doesn't close connection or perform commit.
    *
    * @throws SQLException
    *          if any errors occurred 
    */
   public void executeCommitScripts() throws SQLException
   {
      executeScripts(commitScripts);
   }

   /**
    * Execute script on database.  
    * 
    * @param scripts
    *          the scripts for execution 
    * @param  isSkipSQLExceprion
    *          boolean, skipping SQLException on rollback.  
    * @throws SQLException
    *          if any exception occurred
    */
   protected void executeScripts(List<String> scripts) throws SQLException
   {
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      // set the new autoCommit mode if need
      // for example, the Sybase is not allowed DDL query (CREATE TABLE, DROP TABLE, etc. ) within a multi-statement transaction
      boolean autoCommit = connection.getAutoCommit();
      if (this.autoCommit != autoCommit)
      {
         connection.setAutoCommit(this.autoCommit);
      }

      Statement st = connection.createStatement();
      try
      {
         for (String scr : scripts)
         {
            String sql = JDBCUtils.cleanWhitespaces(scr.trim());
            if (sql.length() > 0)
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Execute script: \n[" + sql + "]");
               }

               executeQuery(st, sql);
            }
         }
      }
      finally
      {
         try
         {
            st.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the Statement." + e);
         }

         // restore previous autoCommit mode
         if (this.autoCommit != autoCommit)
         {
            connection.setAutoCommit(autoCommit);
         }
      }
   }

   protected void executeQuery(final Statement statement, final String sql) throws SQLException
   {
      SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            statement.executeUpdate(sql);
            return null;
         }
      });
   }
}
