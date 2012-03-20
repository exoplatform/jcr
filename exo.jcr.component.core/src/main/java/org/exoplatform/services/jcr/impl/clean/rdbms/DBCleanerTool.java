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
import java.util.Collection;
import java.util.List;

/**
 * The goal of this class is removing data from database.
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleanerTool.java 3769 2011-01-04 15:36:06Z areshetnyak $
 */
public class DBCleanerTool
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleaner");

   protected final Connection connection;

   protected final boolean autoCommit;

   protected final List<String> rollbackingScripts = new ArrayList<String>();

   protected final List<String> committingScripts = new ArrayList<String>();

   protected final List<String> cleaningScripts = new ArrayList<String>();

   /**
    * DBCleanerTool constructor.
    * 
    * @param connection 
    *          connection to database which will be used in cleaning, take in account DBCleanerTool does not
    *          close connection
    * @param autoCommit 
    *          auto commit mode which will be set during script execution         
    */
   DBCleanerTool(Connection connection, boolean autoCommit, Collection<String> cleaningScripts,
      Collection<String> committingScripts, Collection<String> rollbackingScripts)
   {
      this.connection = connection;
      this.autoCommit = autoCommit;

      this.cleaningScripts.addAll(cleaningScripts);
      this.committingScripts.addAll(committingScripts);
      this.rollbackingScripts.addAll(rollbackingScripts);
   }

   /**
    * Clean JCR tables, can't contain some indexes or constraints,
    * should be added on {@link #commit}. It done for possibility to restore
    * data without any violations.
    * 
    * <br>
    * This method does not invoke commit or rollback on {@link Connection} but
    * needed autocommit mode can be set.
    * 
    * @throws DBCleanException
    */
   public void clean() throws DBCleanException
   {
      try
      {
         execute(cleaningScripts);
      }
      catch (SQLException e)
      {
         throw new DBCleanException(JDBCUtils.getFullMessage(e), e);
      }
   }

   /**
    * Executes SQL scripts for finishing clean operations if needed. 
    * It can be adding indexes, constraints, removing temporary objects etc 
    * (related to specific database) or does nothing.
    * 
    * <br>
    * This method does not invoke commit or rollback on {@link Connection} but
    * needed autocommit mode can be set.
    * 
    * @throws DBCleanException
    */
   public void commit() throws DBCleanException
   {
      try
      {
         execute(committingScripts);
      }
      catch (SQLException e)
      {
         throw new DBCleanException(JDBCUtils.getFullMessage(e), e);
      }
   }

   /**
    * Tries to restore previous data by renaming tables etc
    * (related to specific database) or does nothing.
    * 
    * <br>
    * This method does not invoke commit or rollback on {@link Connection} but
    * needed autocommit mode can be set.
    * 
    * @throws DBCleanException
    */
   public void rollback() throws DBCleanException
   {
      try
      {
         execute(rollbackingScripts);
      }
      catch (SQLException e)
      {
         throw new DBCleanException(JDBCUtils.getFullMessage(e), e);
      }
   }

   /**
    * Return connection which it is used in cleaner.
    */
   public Connection getConnection()
   {
      return connection;
   }

   /**
    * Execute script on database. Set auto commit mode if needed.  
    * 
    * @param scripts
    *          the scripts for execution 
    * @throws SQLException
    */
   protected void execute(List<String> scripts) throws SQLException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      // set needed auto commit mode
      boolean autoCommit = connection.getAutoCommit();
      if (autoCommit != this.autoCommit)
      {
         connection.setAutoCommit(this.autoCommit);
      }

      Statement st = connection.createStatement();
      try
      {
         for (String scr : scripts)
         {
            String sql = JDBCUtils.cleanWhitespaces(scr.trim());
            if (!sql.isEmpty())
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
            LOG.error("Can't close the Statement." + e.getMessage());
         }

         // restore previous auto commit mode
         if (autoCommit != this.autoCommit)
         {
            connection.setAutoCommit(autoCommit);
         }
      }
   }

   protected void executeQuery(final Statement statement, final String sql) throws SQLException
   {
      try
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
      catch (SQLException e)
      {
         LOG.error("Query execution \"" + sql + "\" failed");
         throw e;
      }
   }
}
