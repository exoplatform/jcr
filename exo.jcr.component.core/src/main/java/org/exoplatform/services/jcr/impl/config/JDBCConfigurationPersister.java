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
package org.exoplatform.services.jcr.impl.config;

import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.config.ConfigurationPersister;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Repository service configuration persister.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JDBCConfigurationPersister.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class JDBCConfigurationPersister implements ConfigurationPersister
{

   protected static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCConfigurationPersister");

   public final static String PARAM_SOURCE_NAME = "source-name";

   public final static String PARAM_DIALECT = "dialect";

   protected static final String CONFIGNAME = "REPOSITORY-SERVICE-WORKING-CONFIG";

   protected static final String C_DATA = "CONFIGDATA";

   protected String configTableName = "JCR_CONFIG";

   protected String sourceName;

   protected String initSQL;

   public class ConfigurationNotFoundException extends RepositoryConfigurationException
   {
      ConfigurationNotFoundException(String m)
      {
         super(m);
      }
   }

   public class ConfigurationNotInitializedException extends RepositoryConfigurationException
   {
      ConfigurationNotInitializedException(String m)
      {
         super(m);
      }
   }

   protected class ConfigDataHolder
   {

      private final byte[] config;

      ConfigDataHolder(InputStream source) throws IOException
      {
         ByteArrayOutputStream configOut = new ByteArrayOutputStream();
         byte[] b = new byte[1024];
         int read = 0;
         while ((read = source.read(b)) > 0)
         {
            configOut.write(b, 0, read);
         }
         this.config = configOut.toByteArray();
      }

      InputStream getStream()
      {
         return new ByteArrayInputStream(config);
      }

      int getLength()
      {
         return config.length;
      }
   }

   public void init(PropertiesParam params) throws RepositoryConfigurationException
   {
      String sourceNameParam = params.getProperty(PARAM_SOURCE_NAME);
      if (sourceNameParam == null)
      {
         sourceNameParam = params.getProperty("sourceName"); // try old, pre 1.9 name
         if (sourceNameParam == null)
         {
            throw new RepositoryConfigurationException("Repository service configuration. Source name ("
               + PARAM_SOURCE_NAME + ") is expected");
         }
      }
      this.sourceName = sourceNameParam;

      String dialect = params.getProperty(PARAM_DIALECT);
      if (dialect == null || DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(dialect))
      {
         Connection conn = null;
         try
         {
            conn = openConnection();
            dialect = DialectDetecter.detect(conn.getMetaData());
         }
         catch (NamingException e)
         {
            throw new RepositoryConfigurationException(e);
         }
         catch (SQLException e)
         {
            throw new RepositoryConfigurationException(e);
         }
         finally
         {
            if (conn != null)
            {
               try
               {
                  conn.close();
               }
               catch (SQLException e)
               {
                  throw new RepositoryConfigurationException(e);
               }
            }
         }
      }

      String binType = "BLOB";
      if (DBConstants.DB_DIALECT_GENERIC.equalsIgnoreCase(dialect)
         || DBConstants.DB_DIALECT_HSQLDB.equalsIgnoreCase(dialect))
      {
         binType = "VARBINARY(102400)"; // 100Kb
      }
      else if (DBConstants.DB_DIALECT_PGSQL.equalsIgnoreCase(dialect))
      {
         configTableName = configTableName.toUpperCase().toLowerCase(); // postgres needs it
         binType = "BYTEA";
      }
      else if (DBConstants.DB_DIALECT_MSSQL.equalsIgnoreCase(dialect))
      {
         binType = "VARBINARY(max)";
      }
      else if (DBConstants.DB_DIALECT_SYBASE.equalsIgnoreCase(dialect))
      {
         binType = "IMAGE";
      }
      else if (DBConstants.DB_DIALECT_INGRES.equalsIgnoreCase(dialect))
      {
         configTableName = configTableName.toUpperCase().toLowerCase(); // ingres needs it
         binType = "LONG BYTE";
      }

      this.initSQL =
         "CREATE TABLE " + configTableName + " (" + "NAME VARCHAR(64) NOT NULL, " + "CONFIG " + binType + " NOT NULL, "
            + "CONSTRAINT JCR_CONFIG_PK PRIMARY KEY(NAME))";
   }

   protected void checkInitialized() throws RepositoryConfigurationException
   {
      if (sourceName == null)
      {
         throw new RepositoryConfigurationException(
            "Repository service configuration persister isn not initialized. Call init() before.");
      }
   }

   protected Connection openConnection() throws NamingException, SQLException
   {
      DataSource ds = (DataSource)new InitialContext().lookup(sourceName);
      return ds.getConnection();
   }

   /**
    * Check if config table already exists
    * 
    * @param con
    */
   protected boolean isDbInitialized(Connection con)
   {
      try
      {
         ResultSet trs = con.getMetaData().getTables(null, null, configTableName, null);
         return trs.next();
      }
      catch (SQLException e)
      {
         return false;
      }
   }

   public boolean hasConfig() throws RepositoryConfigurationException
   {

      checkInitialized();

      try
      {
         Connection con = openConnection();
         ResultSet res = null;
         PreparedStatement ps = null;
         try
         {
            if (isDbInitialized(con))
            {
               // check that data exists
               ps = con.prepareStatement("SELECT COUNT(*) FROM " + configTableName + " WHERE NAME=?");

               ps.setString(1, CONFIGNAME);
               res = ps.executeQuery();
               if (res.next())
               {
                  return res.getInt(1) > 0;
               }
            }
            return false;
         }
         finally
         {
            if (res != null)
            {
               try
               {
                  res.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the ResultSet: " + e);
               }
            }

            if (ps != null)
            {
               try
               {
                  ps.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the Statement: " + e);
               }
            }

            con.close();
         }
      }
      catch (final SQLException e)
      {
         throw new RepositoryConfigurationException("Database exception. " + e, e);
      }
      catch (final NamingException e)
      {
         throw new RepositoryConfigurationException("JDNI exception. " + e, e);
      }
   }

   public InputStream read() throws RepositoryConfigurationException
   {

      checkInitialized();

      try
      {
         Connection con = openConnection();
         ResultSet res = null;
         PreparedStatement ps = null;
         try
         {
            if (isDbInitialized(con))
            {

               ps = con.prepareStatement("SELECT * FROM " + configTableName + " WHERE NAME=?");
               ps.setString(1, CONFIGNAME);
               res = ps.executeQuery();

               if (res.next())
               {
                  ConfigDataHolder config = new ConfigDataHolder(res.getBinaryStream("config"));
                  return config.getStream();
               }
               else
                  throw new ConfigurationNotFoundException("No configuration data is found in database. Source name "
                     + sourceName);
            }
            else
               throw new ConfigurationNotInitializedException(
                  "Configuration table not is found in database. Source name " + sourceName);

         }
         finally
         {
            if (res != null)
            {
               try
               {
                  res.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the ResultSet: " + e);
               }
            }

            if (ps != null)
            {
               try
               {
                  ps.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the Statement: " + e);
               }
            }

            con.close();
         }
      }
      catch (final IOException e)
      {
         throw new RepositoryConfigurationException("Configuration read exception. " + e, e);
      }
      catch (final SQLException e)
      {
         throw new RepositoryConfigurationException("Database exception. " + e, e);
      }
      catch (final NamingException e)
      {
         throw new RepositoryConfigurationException("JDNI exception. " + e, e);
      }
   }

   public void write(InputStream confData) throws RepositoryConfigurationException
   {

      checkInitialized();

      String sql = null;
      try
      {
         Connection con = openConnection();
         PreparedStatement ps = null;
         try
         {

            con.setAutoCommit(false);

            if (!isDbInitialized(con))
            {
               // init db
               Statement st = con.createStatement();
               st.executeUpdate(sql = initSQL);
               st.close();

               con.commit();
               con.close();

               // one new conn
               con = openConnection();
               con.setAutoCommit(false);
            }

            if (isDbInitialized(con))
            {
               ConfigDataHolder config = new ConfigDataHolder(confData);

               if (hasConfig())
               {
                  sql = "UPDATE " + configTableName + " SET CONFIG=? WHERE NAME=?";
                  ps = con.prepareStatement(sql);
                  ps.setBinaryStream(1, config.getStream(), config.getLength());
                  ps.setString(2, CONFIGNAME);
               }
               else
               {
                  sql = "INSERT INTO " + configTableName + " (NAME, CONFIG) VALUES (?,?)";
                  ps = con.prepareStatement(sql);
                  ps.setString(1, CONFIGNAME);
                  ps.setBinaryStream(2, config.getStream(), config.getLength());
               }

               if (ps.executeUpdate() <= 0)
               {
                  LOG
                     .warn("Repository service configuration doesn't stored ok. No rows was affected in JDBC operation. Datasource "
                        + sourceName + ". SQL: " + sql);
               }
            }
            else
               throw new ConfigurationNotInitializedException(
                  "Configuration table can not be created in database. Source name " + sourceName + ". SQL: " + sql);

            con.commit();

         }
         finally
         {
            if (ps != null)
            {
               try
               {
                  ps.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the Statement: " + e);
               }
            }

            con.close();
         }
      }
      catch (final IOException e)
      {
         throw new RepositoryConfigurationException("Configuration read exception. " + e, e);
      }
      catch (final SQLException e)
      {
         throw new RepositoryConfigurationException("Database exception. " + e + ". SQL: " + sql, e);
      }
      catch (final NamingException e)
      {
         throw new RepositoryConfigurationException("JDNI exception. " + e, e);
      }
   }

}
