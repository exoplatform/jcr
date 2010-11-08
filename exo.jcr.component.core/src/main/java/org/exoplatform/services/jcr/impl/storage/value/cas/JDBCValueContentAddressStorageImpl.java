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
package org.exoplatform.services.jcr.impl.storage.value.cas;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS .<br/>
 * 
 * Stored CAS table in JDBC database.<br/>
 * 
 * NOTE! To make SQL commands compatible with possible ALL RDBMS we use objects names in
 * <strong>!lowercase!</strong>.<br/>
 * 
 * Date: 18.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JDBCValueContentAddressStorageImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class JDBCValueContentAddressStorageImpl implements ValueContentAddressStorage
{

   /**
    * JDBC DataSource name for lookup in JNDI.
    */
   public static final String JDBC_SOURCE_NAME_PARAM = "jdbc-source-name";

   /**
    * JDBC dialect to work with DataSource.
    */
   public static final String JDBC_DIALECT_PARAM = "jdbc-dialect";

   /**
    * It's possible reassign VCAS table name with this parameter. For development purpose!
    */
   public static final String TABLE_NAME_PARAM = "jdbc-table-name";

   /**
    * Default VCAS table name.
    */
   public static final String DEFAULT_TABLE_NAME = "JCR_VCAS";

   /**
    * LOG.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCValueContentAddressStorageImpl");

   /**
    * MYSQL_PK_CONSTRAINT_DETECT_PATTERN.
    */
   private static final String MYSQL_PK_CONSTRAINT_DETECT_PATTERN =
      "(.*Constraint+.*Violation+.*Duplicate+.*entry+.*)+?";

   /**
    * MYSQL_PK_CONSTRAINT_DETECT.
    */
   private static final Pattern MYSQL_PK_CONSTRAINT_DETECT =
      Pattern.compile(MYSQL_PK_CONSTRAINT_DETECT_PATTERN, Pattern.CASE_INSENSITIVE);

   /**
    * DB2_PK_CONSTRAINT_DETECT_PATTERN.
    * %s must be replaced with original table name before compile Pattern.
    */
   private static final String DB2_PK_CONSTRAINT_DETECT_PATTERN =
      "(.*DB2 SQL [Ee]rror+.*SQLCODE[:=].?-803+.*SQLSTATE[:=].?23505+.*%s.*)+?";

   /**
    * MYSQL_PK_CONSTRAINT_DETECT_PATTERN.
    */
   private static final String H2_PK_CONSTRAINT_DETECT_PATTERN = "(.*JdbcSQLException.*violation.*PRIMARY_KEY_.*)";

   /**
    * H2_PK_CONSTRAINT_DETECT.
    */
   private static final Pattern H2_PK_CONSTRAINT_DETECT =
      Pattern.compile(H2_PK_CONSTRAINT_DETECT_PATTERN, Pattern.CASE_INSENSITIVE);

   /**
    * DB2_PK_CONSTRAINT_DETECT.
    */
   private Pattern DB2_PK_CONSTRAINT_DETECT;

   protected DataSource dataSource;

   protected String tableName;

   protected String dialect;

   protected String sqlAddRecord;

   protected String sqlDeleteRecord;

   protected String sqlDeleteValueRecord;

   protected String sqlSelectRecord;

   protected String sqlSelectRecords;

   protected String sqlSelectOwnRecords;

   protected String sqlSelectSharingProps;

   protected String sqlConstraintPK;

   protected String sqlVCASIDX;

   /**
    * {@inheritDoc}
    */
   public void init(Properties props) throws RepositoryConfigurationException, VCASException
   {
      final String sn = props.getProperty(JDBC_SOURCE_NAME_PARAM);
      if (sn == null)
      {
         throw new RepositoryConfigurationException(JDBC_SOURCE_NAME_PARAM + " parameter expected!");
      }

      try
      {
         dataSource = (DataSource)new InitialContext().lookup(sn);
         Connection conn = null;
         Statement st = null;
         try
         {
            PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  return dataSource.getConnection();
               }
            };
            try
            {
               conn = (Connection)AccessController.doPrivileged(action);
            }
            catch (PrivilegedActionException pae)
            {
               Throwable cause = pae.getCause();
               if (cause instanceof SQLException)
               {
                  throw (SQLException)cause;
               }
               else if (cause instanceof RuntimeException)
               {
                  throw (RuntimeException)cause;
               }
               else
               {
                  throw new RuntimeException(cause);
               }
            }

            DatabaseMetaData dbMetaData = conn.getMetaData();

            String dialect = props.getProperty(JDBC_DIALECT_PARAM);
            if (dialect == null || DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(dialect))
            {
               dialect = DialectDetecter.detect(dbMetaData);
            }
            this.dialect = dialect;

            // init database metadata
            String tn = props.getProperty(TABLE_NAME_PARAM);
            if (tn != null)
            {
               tableName = tn;
            }
            else
            {
               tableName = DEFAULT_TABLE_NAME;
            }

            // make error pattern for DB2
            String pattern = String.format(DB2_PK_CONSTRAINT_DETECT_PATTERN, tableName);

            DB2_PK_CONSTRAINT_DETECT = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

            sqlConstraintPK = tableName + "_PK";

            sqlVCASIDX = tableName + "_IDX";

            if (DBConstants.DB_DIALECT_PGSQL.equalsIgnoreCase(dialect)
               || DBConstants.DB_DIALECT_INGRES.equalsIgnoreCase(dialect))
            {
               // use lowercase for postgres/ingres metadata.getTable(), HSQLDB wants UPPERCASE
               // for other seems not matter
               tableName = tableName.toUpperCase().toLowerCase();
               sqlConstraintPK = sqlConstraintPK.toUpperCase().toLowerCase();
               sqlVCASIDX = sqlVCASIDX.toUpperCase().toLowerCase();
            }

            sqlAddRecord = "INSERT INTO " + tableName + " (PROPERTY_ID, ORDER_NUM, CAS_ID) VALUES(?,?,?)";
            sqlDeleteRecord = "DELETE FROM " + tableName + " WHERE PROPERTY_ID=?";
            sqlDeleteValueRecord = "DELETE FROM " + tableName + " WHERE PROPERTY_ID=? AND ORDER_NUM=?";
            sqlSelectRecord = "SELECT CAS_ID FROM " + tableName + " WHERE PROPERTY_ID=? AND ORDER_NUM=?";
            sqlSelectRecords = "SELECT CAS_ID, ORDER_NUM FROM " + tableName + " WHERE PROPERTY_ID=? ORDER BY ORDER_NUM";

            sqlSelectOwnRecords =
               "SELECT P.CAS_ID, P.ORDER_NUM, S.CAS_ID as SHARED_ID " + "FROM " + tableName + " P LEFT JOIN "
                  + tableName + " S ON P.PROPERTY_ID<>S.PROPERTY_ID AND P.CAS_ID=S.CAS_ID "
                  + "WHERE P.PROPERTY_ID=? GROUP BY P.CAS_ID, P.ORDER_NUM, S.CAS_ID ORDER BY P.ORDER_NUM";

            sqlSelectSharingProps =
               "SELECT DISTINCT C.PROPERTY_ID AS PROPERTY_ID FROM " + tableName + " C, " + tableName + " P "
                  + "WHERE C.CAS_ID=P.CAS_ID AND C.PROPERTY_ID<>P.PROPERTY_ID AND P.PROPERTY_ID=?";

            // init database objects
            ResultSet trs = dbMetaData.getTables(null, null, tableName, null);
            // check if table already exists
            if (!trs.next())
            {
               st = conn.createStatement();

               // create table
               st.executeUpdate("CREATE TABLE " + tableName
                  + " (PROPERTY_ID VARCHAR(96) NOT NULL, ORDER_NUM INTEGER NOT NULL, CAS_ID VARCHAR(512) NOT NULL, "
                  + "CONSTRAINT " + sqlConstraintPK + " PRIMARY KEY(PROPERTY_ID, ORDER_NUM))");

               // create index on hash (CAS_ID)
               st.executeUpdate("CREATE INDEX " + sqlVCASIDX + " ON " + tableName + "(CAS_ID, PROPERTY_ID, ORDER_NUM)");

               if (LOG.isDebugEnabled())
               {
                  LOG.debug("JDBC Value Content Address Storage initialized in database " + sn);
               }
            }
            else if (LOG.isDebugEnabled())
            {
               LOG.debug("JDBC Value Content Address Storage already initialized in database " + sn);
            }
         }
         catch (SQLException e)
         {
            throw new VCASException("VCAS INIT database error: " + e, e);
         }
         finally
         {
            if (st != null)
            {
               try
               {
                  st.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the Statement: " + e);
               }
            }

            if (conn != null)
            {
               try
               {
                  conn.close();
               }
               catch (SQLException e)
               {
                  throw new VCASException("VCAS INIT database error on Connection close: " + e, e);
               }
            }
         }
      }
      catch (NamingException e)
      {
         throw new RepositoryConfigurationException("JDBC data source is not available in JNDI with name '" + sn
            + "'. Error: " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addValue(String propertyId, int orderNum, String identifier) throws VCASException
   {

      try
      {
         Connection con = dataSource.getConnection();
         try
         {
            PreparedStatement ps = con.prepareStatement(sqlAddRecord);
            ps.setString(1, propertyId);
            ps.setInt(2, orderNum);
            ps.setString(3, identifier);
            ps.executeUpdate();
            ps.close();
         }
         finally
         {
            con.close();
         }
      }
      catch (SQLException e)
      {
         // check is it a primary key vioaltion or smth else
         // if primary key - it's record already exists issue, VCAS error otherwise.
         if (isRecordAlreadyExistsException(e))
         {
            throw new RecordAlreadyExistsException("Record already exists, propertyId=" + propertyId + " orderNum="
               + orderNum + ". Error: " + e, e);
         }

         throw new VCASException("VCAS ADD database error: " + e, e);
      }
   }

   /**
    * Tell is it a RecordAlreadyExistsException.
    * 
    * @param e
    *          SQLException
    * @return boolean
    */
   private boolean isRecordAlreadyExistsException(SQLException e)
   {
      // Search in UPPER case
      // MySQL 5.0.x - com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException:
      // Duplicate entry '4f684b34c0a800030018c34f99165791-0' for key 1
      // HSQLDB 8.x - java.sql.SQLException: Violation of unique constraint $$: duplicate value(s) for
      // column(s) $$:
      // JCR_VCAS_PK in statement [INSERT INTO JCR_VCAS (PROPERTY_ID, ORDER_NUM, CAS_ID)
      // VALUES(?,?,?)]         String H2_PK_CONSTRAINT_DETECT_PATTERN = "(.*JdbcSQLException.*violation.*PRIMARY_KEY_.*)";
      // PostgreSQL 8.2.x - org.postgresql.util.PSQLException: ERROR: duplicate key violates unique
      // constraint "jcr_vcas_pk"
      // Oracle 9i x64 (on Fedora 7) - java.sql.SQLException: ORA-00001: unique constraint
      // (EXOADMIN.JCR_VCAS_PK) violated
      // H2 - org.h2.jdbc.JdbcSQLException: Unique index or primary key violation: 
      // "PRIMARY_KEY_4 ON PUBLIC.JCR_VCAS_TEST(PROPERTY_ID, ORDER_NUM)"; 
      //
      String err = e.toString();
      if (DBConstants.DB_DIALECT_MYSQL.equalsIgnoreCase(dialect)
         || DBConstants.DB_DIALECT_MYSQL_UTF8.equalsIgnoreCase(dialect))
      {
         // for MySQL will search
         return MYSQL_PK_CONSTRAINT_DETECT.matcher(err).find();
      }
      else if (err.toLowerCase().toUpperCase().indexOf(sqlConstraintPK.toLowerCase().toUpperCase()) >= 0)
      {
         // most of supported dbs prints PK name in exception
         return true;
      }
      else if (DBConstants.DB_DIALECT_DB2.equalsIgnoreCase(dialect))
      {
         return DB2_PK_CONSTRAINT_DETECT.matcher(err).find();
      }
      else if (DBConstants.DB_DIALECT_H2.equalsIgnoreCase(dialect))
      {
         return H2_PK_CONSTRAINT_DETECT.matcher(err).find();
      }

      // NOTICE! As an additional check we may ask the database for property currently processed in
      // VCAS
      // and tell true if the property already exists only.

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void deleteProperty(String propertyId) throws VCASException
   {
      try
      {
         Connection con = dataSource.getConnection();
         try
         {
            PreparedStatement ps = con.prepareStatement(sqlDeleteRecord);
            ps.setString(1, propertyId);
            int res = ps.executeUpdate();
            ps.close();

            if (res <= 0)
            {
               throw new RecordNotFoundException("Record not found, propertyId=" + propertyId);
            }
         }
         finally
         {
            con.close();
         }
      }
      catch (SQLException e)
      {
         throw new VCASException("VCAS DELETE database error: " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void deleteValue(String propertyId, int orderNumb) throws VCASException
   {
      try
      {
         Connection con = dataSource.getConnection();
         try
         {
            PreparedStatement ps = con.prepareStatement(sqlDeleteValueRecord);
            ps.setString(1, propertyId);
            ps.setInt(2, orderNumb);
            int res = ps.executeUpdate();
            ps.close();

            if (res <= 0)
            {
               throw new RecordNotFoundException("Value record not found, propertyId=" + propertyId + " orderNumb="
                  + orderNumb);
            }
         }
         finally
         {
            con.close();
         }
      }
      catch (SQLException e)
      {
         throw new VCASException("VCAS Value DELETE database error: " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getIdentifier(String propertyId, int orderNum) throws VCASException
   {
      try
      {
         Connection con = dataSource.getConnection();
         ResultSet rs = null;
         PreparedStatement ps = null;
         try
         {
            ps = con.prepareStatement(sqlSelectRecord);
            ps.setString(1, propertyId);
            ps.setInt(2, orderNum);
            rs = ps.executeQuery();

            if (rs.next())
            {
               return rs.getString("CAS_ID");
            }
            else
            {
               throw new RecordNotFoundException("No record found with propertyId=" + propertyId + " orderNum="
                  + orderNum);
            }
         }
         finally
         {
            if (rs != null)
            {
               try
               {
                  rs.close();
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
      catch (SQLException e)
      {
         throw new VCASException("VCAS GET ID database error: " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<String> getIdentifiers(String propertyId, boolean ownOnly) throws VCASException
   {
      try
      {
         Connection con = dataSource.getConnection();
         PreparedStatement ps = null;
         ResultSet rs = null;
         try
         {
            List<String> ids = new ArrayList<String>();

            if (ownOnly)
            {
               ps = con.prepareStatement(sqlSelectOwnRecords);
               ps.setString(1, propertyId);
               rs = ps.executeQuery();
               if (rs.next())
               {
                  do
                  {
                     rs.getString("SHARED_ID");
                     if (rs.wasNull())
                        ids.add(rs.getString("CAS_ID"));
                  }
                  while (rs.next());
                  return ids;
               }
               else
               {
                  throw new RecordNotFoundException("No records found with propertyId=" + propertyId);
               }
            }
            else
            {
               // TODO unused externaly feature (except tests)
               ps = con.prepareStatement(sqlSelectRecords);
               ps.setString(1, propertyId);
               rs = ps.executeQuery();
               if (rs.next())
               {
                  do
                  {
                     ids.add(rs.getString("CAS_ID"));
                  }
                  while (rs.next());
                  return ids;
               }
               else
               {
                  throw new RecordNotFoundException("No records found with propertyId=" + propertyId);
               }
            }
         }
         finally
         {
            if (rs != null)
            {
               try
               {
                  rs.close();
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
      catch (SQLException e)
      {
         throw new VCASException("VCAS GET IDs database error: " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasSharedContent(String propertyId) throws VCASException
   {
      try
      {
         Connection con = dataSource.getConnection();
         PreparedStatement ps = null;
         try
         {
            ps = con.prepareStatement(sqlSelectSharingProps);
            ps.setString(1, propertyId);
            return ps.executeQuery().next();
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
      catch (SQLException e)
      {
         throw new VCASException("VCAS HAS SHARED IDs database error: " + e, e);
      }
   }
}
