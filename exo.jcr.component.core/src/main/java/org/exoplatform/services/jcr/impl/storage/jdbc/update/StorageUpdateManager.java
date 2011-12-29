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
package org.exoplatform.services.jcr.impl.storage.jdbc.update;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<p>
 * This feature is deprecated and going to be removed in 1.15 version.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: StorageUpdateManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
@Deprecated
public class StorageUpdateManager
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.StorageUpdateManager");

   public static final String STORAGE_VERSION_1_0_0 = "1.0";

   public static final String STORAGE_VERSION_1_0_1 = "1.0.1";

   public static final String STORAGE_VERSION_1_1_0 = "1.1";

   public static final String STORAGE_VERSION_1_5_0 = "1.5";

   public static final String STORAGE_VERSION_1_6_0 = "1.6";

   public static final String STORAGE_VERSION_1_7_0 = "1.7";

   public static final String FIRST_STORAGE_VERSION = STORAGE_VERSION_1_0_0;

   public static final String PREV_STORAGE_VERSION = STORAGE_VERSION_1_6_0;

   public static final String REQUIRED_STORAGE_VERSION = STORAGE_VERSION_1_7_0;

   protected final String SQL_INSERT_VERSION;

   protected static final String SQL_INSERT_VERSION_MULTIDB = "insert into JCR_MCONTAINER(VERSION) values(?)";

   protected static final String SQL_INSERT_VERSION_SINGLEDB = "insert into JCR_SCONTAINER(VERSION) values(?)";

   protected final String SQL_UPDATE_VERSION;

   protected static final String SQL_UPDATE_VERSION_MULTIDB = "update JCR_MCONTAINER set VERSION=?";

   protected static final String SQL_UPDATE_VERSION_SINGLEDB = "update JCR_SCONTAINER set VERSION=?";

   protected final String SQL_SELECT_VERSION;

   protected static final String SQL_SELECT_VERSION_MULTIDB = "select VERSION from JCR_MCONTAINER";

   protected static final String SQL_SELECT_VERSION_SINGLEDB = "select VERSION from JCR_SCONTAINER";

   protected static final String SQL_UPDATE_JCRUUID_MULTIDB = "update JCR_MVALUE set DATA=? where ID=?";

   protected static final String SQL_UPDATE_JCRUUID_SINGLEDB = "update JCR_SVALUE set DATA=? where ID=?";

   protected static final String SQL_SELECT_JCRUUID_MULTIDB =
      "select I.PATH, N.ID as NID, V.ID as VID, V.DATA from JCR_MITEM I, JCR_MNODE N, JCR_MPROPERTY P, JCR_MVALUE V "
         + "WHERE I.ID = P.ID and N.ID = P.PARENT_ID and P.ID = V.PROPERTY_ID and " + "I.PATH like '%"
         + Constants.JCR_UUID.getAsString() + "%' " + "order by V.ID";

   protected static final String SQL_SELECT_JCRUUID_SINGLEDB =
      "select I.PATH, N.ID as NID, V.ID as VID, V.DATA from JCR_SITEM I, JCR_SNODE N, JCR_SPROPERTY P, JCR_SVALUE V "
         + "WHERE I.ID = P.ID and N.ID = P.PARENT_ID and P.ID = V.PROPERTY_ID and " + "I.PATH like '%"
         + Constants.JCR_UUID.getAsString() + "%' " + "order by V.ID";

   protected static final String FROZENJCRUUID = "$FROZENJCRUUID$";

   protected static final String SQL_SELECT_FROZENJCRUUID_MULTIDB =
      "select I.PATH, N.ID as NID, V.ID as VID, V.DATA from JCR_MITEM I, JCR_MNODE N, JCR_MPROPERTY P, JCR_MVALUE V "
         + "WHERE I.ID = P.ID and N.ID = P.PARENT_ID and P.ID = V.PROPERTY_ID and " + "I.PATH like '" + FROZENJCRUUID
         + "' " + "order by V.ID";

   protected static final String SQL_SELECT_FROZENJCRUUID_SINGLEDB =
      "select I.PATH, N.ID as NID, V.ID as VID, V.DATA from JCR_SITEM I, JCR_SNODE N, JCR_SPROPERTY P, JCR_SVALUE V "
         + "WHERE I.ID = P.ID and N.ID = P.PARENT_ID and P.ID = V.PROPERTY_ID and " + "I.PATH like '" + FROZENJCRUUID
         + "' " + "order by V.ID";

   protected static final String SQL_SELECT_REFERENCES_MULTIDB =
      "select I.PATH, V.PROPERTY_ID, V.ORDER_NUM, V.DATA" + " from JCR_MITEM I, JCR_MPROPERTY P, JCR_MVALUE V"
         + " where I.ID=P.ID and P.ID=V.PROPERTY_ID and P.TYPE=" + PropertyType.REFERENCE
         + " order by I.ID, V.ORDER_NUM";

   protected static final String SQL_SELECT_REFERENCES_SINGLEDB =
      "select I.PATH, V.PROPERTY_ID, V.ORDER_NUM, V.DATA" + " from JCR_SITEM I, JCR_SPROPERTY P, JCR_SVALUE V"
         + " where I.ID=P.ID and P.ID=V.PROPERTY_ID and P.TYPE=" + PropertyType.REFERENCE
         // + " and I.CONTAINER_NAME=?"
         // // An UUID contains
         // container name as prefix
         + " order by I.ID, V.ORDER_NUM";

   protected static final String SQL_INSERT_REFERENCES_MULTIDB =
      "insert into JCR_MREF (NODE_ID, PROPERTY_ID, ORDER_NUM) values(?,?,?)";

   protected static final String SQL_INSERT_REFERENCES_SINGLEDB =
      "insert into JCR_SREF (NODE_ID, PROPERTY_ID, ORDER_NUM) values(?,?,?)";

   protected final String SQL_SELECT_JCRUUID;

   protected final String SQL_SELECT_FROZENJCRUUID;

   protected final String SQL_UPDATE_JCRUUID;

   protected final String SQL_SELECT_REFERENCES;

   protected final String SQL_INSERT_REFERENCES;

   private final Connection connection;

   private final String sourceName;

   private final boolean multiDB;

   private class JcrIdentifier
   {

      private final String path;

      private final String nodeIdentifier;

      private final String jcrIdentifier;

      private final String valueId;

      public JcrIdentifier(String path, String nodeIdentifier, String valueId, InputStream valueData)
         throws IOException
      {
         this.path = path;
         this.nodeIdentifier = nodeIdentifier;
         this.valueId = valueId;
         this.jcrIdentifier = new String(readIdentifierStream(valueData));
      }

      public String getNodeIdentifier()
      {
         return nodeIdentifier;
      }

      public String getJcrIdentifier()
      {
         return jcrIdentifier;
      }

      public String getPath()
      {
         return path;
      }

      public String getValueId()
      {
         return valueId;
      }
   }

   private abstract class Updater
   {

      protected abstract void updateBody(Connection conn) throws SQLException;

      public void update() throws SQLException
      {
         try
         {
            // fix before the version update
            updateBody(connection);

            PreparedStatement insertVersion = connection.prepareStatement(SQL_UPDATE_VERSION);
            insertVersion.setString(1, REQUIRED_STORAGE_VERSION);
            insertVersion.executeUpdate();
            insertVersion.close();

            connection.commit();
         }
         catch (Exception e)
         {
            try
            {
               connection.rollback();
            }
            catch (SQLException sqle)
            {
               log.warn("Error of update rollback: " + sqle.getMessage(), sqle);
            }
         }
      }
   }

   private class Updater100 extends Updater // NOSONAR
   {

      @Override
      public void updateBody(Connection conn) throws SQLException
      {
         fixCopyIdentifierBug(conn); // to 1.0.1
         fillReferences(conn); // to 1.1
      }
   }

   private class Updater101 extends Updater // NOSONAR
   {

      @Override
      public void updateBody(Connection conn) throws SQLException
      {
         fillReferences(conn);
      }
   }

   private StorageUpdateManager(String sourceName, Connection connection, boolean multiDB) throws SQLException
   {
      this.connection = connection;
      this.sourceName = sourceName;
      this.multiDB = multiDB;

      this.SQL_SELECT_VERSION = multiDB ? SQL_SELECT_VERSION_MULTIDB : SQL_SELECT_VERSION_SINGLEDB;
      this.SQL_INSERT_VERSION = multiDB ? SQL_INSERT_VERSION_MULTIDB : SQL_INSERT_VERSION_SINGLEDB;
      this.SQL_UPDATE_VERSION = multiDB ? SQL_UPDATE_VERSION_MULTIDB : SQL_UPDATE_VERSION_SINGLEDB;

      this.SQL_SELECT_JCRUUID = multiDB ? SQL_SELECT_JCRUUID_MULTIDB : SQL_SELECT_JCRUUID_SINGLEDB;
      this.SQL_SELECT_FROZENJCRUUID = multiDB ? SQL_SELECT_FROZENJCRUUID_MULTIDB : SQL_SELECT_FROZENJCRUUID_SINGLEDB;
      this.SQL_UPDATE_JCRUUID = multiDB ? SQL_UPDATE_JCRUUID_MULTIDB : SQL_UPDATE_JCRUUID_SINGLEDB;
      this.SQL_SELECT_REFERENCES = multiDB ? SQL_SELECT_REFERENCES_MULTIDB : SQL_SELECT_REFERENCES_SINGLEDB;
      this.SQL_INSERT_REFERENCES = multiDB ? SQL_INSERT_REFERENCES_MULTIDB : SQL_INSERT_REFERENCES_SINGLEDB;
   }

   /**
    * Check current storage version and update if updateNow==true
    * <p>
    * This feature is deprecated and going to be removed in 1.15 version.
    * 
    * @param ds
    * @param updateNow
    * @return
    * @throws RepositoryException
    */
   @Deprecated
   public static synchronized String checkVersion(String sourceName, Connection connection, boolean multiDB,
      boolean updateNow) throws RepositoryException
   {
      try
      {
         connection.setAutoCommit(false);

         StorageUpdateManager manager = new StorageUpdateManager(sourceName, connection, multiDB);
         String version = manager.currentVersion();
         connection.commit();
         return version;
      }
      catch (Exception e)
      {
         try
         {
            connection.rollback();
         }
         catch (SQLException er)
         {
            log.warn("Error of connection rollback (close) " + er, er);
         }
         return Constants.UNKNOWN;
      }
      finally
      {
         try
         {
            connection.close();
         }
         catch (SQLException e)
         {
            log.warn("Error of connection finalyzation (close) " + e, e);
         }
      }
   }



   /**
    * @return current storage version
    * @throws SQLException
    */
   private String currentVersion() throws SQLException
   {
      ResultSet version = null;
      Statement st = null;
      try
      {
         st = connection.createStatement();
         version = st.executeQuery(SQL_SELECT_VERSION);
         if (version.next())
            return version.getString("VERSION");
      }
      catch (SQLException e)
      {
         return FIRST_STORAGE_VERSION;
      }
      finally
      {
         if (version != null)
         {
            try
            {
               version.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the ResultSet: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the Statement: " + e);
            }
         }
      }

      PreparedStatement insertVersion = connection.prepareStatement(SQL_INSERT_VERSION);
      insertVersion.setString(1, REQUIRED_STORAGE_VERSION);
      insertVersion.executeUpdate();
      insertVersion.close();
      return REQUIRED_STORAGE_VERSION;
   }

   /**
    * fix data in the container
    * 
    * @throws SQLException
    */
   private void fixCopyIdentifierBug(Connection conn) throws SQLException
   {
      // need to search all referenceable nodes and fix their
      // property jcr:uuid with valid value (ID column of JCR_xITEM)

      ResultSet refs = null;
      PreparedStatement update = null;
      Statement st = null;
      try
      {
         st = conn.createStatement();
         refs = st.executeQuery(SQL_SELECT_JCRUUID);
         update = conn.prepareStatement(SQL_UPDATE_JCRUUID);
         while (refs.next())
         {
            try
            {
               JcrIdentifier jcrIdentifier =
                  new JcrIdentifier(refs.getString("PATH"), refs.getString("NID"), refs.getString("VID"), refs
                     .getBinaryStream("DATA"));
               if (!jcrIdentifier.getNodeIdentifier().equals(jcrIdentifier.getJcrIdentifier()))
               {
                  log.info("STORAGE UPDATE >>>: Property jcr:uuid have to be updated with actual value. Property: "
                     + jcrIdentifier.getPath() + ", actual:" + jcrIdentifier.getNodeIdentifier() + ", existed: "
                     + jcrIdentifier.getJcrIdentifier());

                  update.clearParameters();
                  update.setBinaryStream(1, new ByteArrayInputStream(jcrIdentifier.getNodeIdentifier().getBytes()),
                     jcrIdentifier.getNodeIdentifier().length());
                  update.setString(2, jcrIdentifier.getValueId());

                  if (update.executeUpdate() != 1)
                  {
                     log
                        .warn("STORAGE UPDATE !!!: More than one jcr:uuid property values were updated. Updated value id: "
                           + jcrIdentifier.getValueId());
                  }
                  else
                  {
                     log.info("STORAGE UPDATE <<<: Property jcr:uuid update successful. Property: "
                        + jcrIdentifier.getPath());
                  }

                  // [PN] 27.09.06 Need to be developed more with common versionHistory (of copied nodes)
                  // etc.
               }
            }
            catch (IOException e)
            {
               log.error("Can't read property value data: " + e.getMessage(), e);
            }
         }
      }
      catch (SQLException e)
      {
         log.error(e.getLocalizedMessage(), e);
      }
      finally
      {
         if (refs != null)
         {
            try
            {
               refs.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the ResultSet: " + e);
            }
         }

         if (update != null)
         {
            try
            {
               update.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the Statement: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   /**
    * fill JCR_XREF table with refernces values from JCR_XVALUE
    * 
    * @throws SQLException
    */
   private void fillReferences(Connection conn) throws SQLException
   {

      ResultSet refs = null;
      PreparedStatement update = null;
      Statement st = null;
      try
      {
         st = conn.createStatement();
         refs = st.executeQuery(SQL_SELECT_REFERENCES);
         update = conn.prepareStatement(SQL_INSERT_REFERENCES);
         while (refs.next())
         {
            try
            {
               String refNodeIdentifier = new String(readIdentifierStream(refs.getBinaryStream("DATA")));
               String refPropertyIdentifier = refs.getString("PROPERTY_ID");
               int refOrderNum = refs.getInt("ORDER_NUM");

               String refPropertyPath = refs.getString("PATH");

               log.info("INSERT REFERENCE >>> Property: " + refPropertyPath + ", " + refPropertyIdentifier + ", "
                  + refOrderNum + "; Node UUID: " + refNodeIdentifier);

               update.clearParameters();
               update.setString(1, refNodeIdentifier);
               update.setString(2, refPropertyIdentifier);
               update.setInt(3, refOrderNum);

               if (update.executeUpdate() != 1)
               {
                  log.warn("INSERT REFERENCE !!!: More than one REFERENCE property was copied");
               }
               else
               {
                  log.info("INSERT REFERENCE <<<: Done. Property: " + refPropertyPath);
               }
            }
            catch (IOException e)
            {
               log.error("Can't read property value data: " + e.getMessage(), e);
            }
         }
      }
      catch (SQLException e)
      {
         log.error("Fill references. Storage update error: " + e.getMessage(), e);
      }
      finally
      {
         if (refs != null)
         {
            try
            {
               refs.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the ResultSet: " + e);
            }
         }

         if (update != null)
         {
            try
            {
               update.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the Statement: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               log.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   private byte[] readIdentifierStream(InputStream stream) throws IOException
   {
      byte[] buf = new byte[IdGenerator.IDENTIFIER_LENGTH];
      stream.read(buf);
      return buf;
   }
}
