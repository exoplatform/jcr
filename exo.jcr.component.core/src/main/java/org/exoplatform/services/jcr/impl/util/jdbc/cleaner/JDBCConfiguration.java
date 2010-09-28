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
package org.exoplatform.services.jcr.impl.util.jdbc.cleaner;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public class JDBCConfiguration
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCConfiguration");

   public final static String SOURCE_NAME = "source-name";

   public final static String MULTIDB = "multi-db";

   public final static String SINGLEDB = "single-db";

   /**
    * Describe which type of RDBMS will be used (DB creation metadata etc.)
    */
   public final static String DB_DIALECT = "dialect";

   public final static String DB_DRIVER = "driverClassName";

   public final static String DB_URL = "url";

   public final static String DB_USERNAME = "username";

   public final static String DB_PASSWORD = "password";

   public final static String DB_FORCE_QUERY_HINTS = "force.query.hints";

   protected final String containerName;

   protected final String dbSourceName;

   protected final boolean multiDb;

   protected final String dbDriver;

   protected final String dbDialect;

   protected final String dbUrl;

   protected final String dbUserName;

   protected final String dbPassword;

   public JDBCConfiguration(WorkspaceEntry wsConfig) throws RepositoryConfigurationException, NamingException,
      RepositoryException, IOException
   {
      this.containerName = wsConfig.getName();
      this.multiDb = Boolean.parseBoolean(wsConfig.getContainer().getParameterValue(MULTIDB));

      // ------------- Database config ------------------
      String pDbDialect = null;
      try
      {
         pDbDialect = validateDialect(wsConfig.getContainer().getParameterValue(DB_DIALECT));
      }
      catch (RepositoryConfigurationException e)
      {
         pDbDialect = DBConstants.DB_DIALECT_GENERIC;
      }

      String pDbDriver = null;
      String pDbUrl = null;
      String pDbUserName = null;
      String pDbPassword = null;
      try
      {
         pDbDriver = wsConfig.getContainer().getParameterValue(DB_DRIVER);

         // username/passwd may not pesent
         try
         {
            pDbUserName = wsConfig.getContainer().getParameterValue(DB_USERNAME);
            pDbPassword = wsConfig.getContainer().getParameterValue(DB_PASSWORD);
         }
         catch (RepositoryConfigurationException e)
         {
            pDbUserName = pDbPassword = null;
         }

         pDbUrl = wsConfig.getContainer().getParameterValue(DB_URL); // last here!
      }
      catch (RepositoryConfigurationException e)
      {
      }

      if (pDbUrl != null)
      {
         this.dbDriver = pDbDriver;
         this.dbUrl = pDbUrl;
         this.dbUserName = pDbUserName;
         this.dbPassword = pDbPassword;
         this.dbSourceName = null;
         LOG.info("Connect to JCR database as user '" + this.dbUserName + "'");

         if (pDbDialect == DBConstants.DB_DIALECT_GENERIC || DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(pDbDialect))
         {
            // try to detect via JDBC metadata
            Connection jdbcConn = null;
            try
            {
               jdbcConn =
                  dbUserName != null ? DriverManager.getConnection(dbUrl, dbUserName, dbPassword) : DriverManager
                     .getConnection(dbUrl);

               this.dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
            }
            catch (SQLException e)
            {
               throw new RepositoryException(e);
            }
            finally
            {
               if (jdbcConn != null)
               {
                  try
                  {
                     jdbcConn.close();
                  }
                  catch (SQLException e)
                  {
                     throw new RepositoryException(e);
                  }
               }
            }
         }
         else
         {
            this.dbDialect = pDbDialect;
         }
      }
      else
      {
         this.dbDriver = null;
         this.dbUrl = null;
         this.dbUserName = null;
         this.dbPassword = null;

         String sn;
         try
         {
            sn = wsConfig.getContainer().getParameterValue(SOURCE_NAME);
         }
         catch (RepositoryConfigurationException e)
         {
            sn = wsConfig.getContainer().getParameterValue("sourceName"); // TODO for backward comp,
            // remove in rel.2.0
         }
         this.dbSourceName = sn;

         if (pDbDialect == DBConstants.DB_DIALECT_GENERIC)
         {
            // try to detect via JDBC metadata
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               Connection jdbcConn = null;
               try
               {
                  jdbcConn = ds.getConnection();
                  this.dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
               }
               catch (SQLException e)
               {
                  throw new RepositoryException(e);
               }
               finally
               {
                  if (jdbcConn != null)
                  {
                     try
                     {
                        jdbcConn.close();
                     }
                     catch (SQLException e)
                     {
                        throw new RepositoryException(e);
                     }
                  }
               }
            }
            else
            {
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
            }
         }
         else
         {
            this.dbDialect = pDbDialect;
         }
      }
      LOG.info("Using a dialect '" + this.dbDialect + "'");
   }

   public String getContainerName()
   {
      return containerName;
   }

   public String getDbSourceName()
   {
      return dbSourceName;
   }

   public boolean isMultiDb()
   {
      return multiDb;
   }

   public String getDbDriver()
   {
      return dbDriver;
   }

   public String getDbDialect()
   {
      return dbDialect;
   }

   public String getDbUrl()
   {
      return dbUrl;
   }

   public String getDbUserName()
   {
      return dbUserName;
   }

   public String getDbPassword()
   {
      return dbPassword;
   }

   protected String validateDialect(String confParam)
   {
      for (String dbType : DBConstants.DB_DIALECTS)
      {
         if (dbType.equalsIgnoreCase(confParam))
         {
            return dbType;
         }
      }

      return DBConstants.DB_DIALECT_GENERIC; // by default
   }

}
