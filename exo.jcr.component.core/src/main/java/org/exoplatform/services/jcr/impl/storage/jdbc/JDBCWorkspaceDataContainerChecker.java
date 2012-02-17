/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.InspectionReport;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 14 жовт. 2011 skarpenko $
 *
 */
public class JDBCWorkspaceDataContainerChecker
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainerChecker");

   /**
    * Check database.
    * <p>
    * Check that database is not broken, and all base relation between jcr-items are not corrupted.
    * </p>
    * 
    * @param inspectionLog - log where inspection results will be placed
    * @return InspectionLog
    * @throws RepositoryException
    * @throws IOException
    */
   public static void checkDataBase(JDBCWorkspaceDataContainer jdbcDataContainer, InspectionReport report)
      throws RepositoryException, IOException
   {
      Set<InspectionQuery> queries = new HashSet<InspectionQuery>();

      // preload queries
      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM I where NOT EXISTS(select * from JCR_MITEM P where P.ID = I.PARENT_ID)"
         : "select * from JCR_SITEM I where I.CONTAINER_NAME='" + jdbcDataContainer.containerName
            + "' and NOT EXISTS(select * from JCR_SITEM P where P.ID = I.PARENT_ID)", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME, DBConstants.COLUMN_CLASS},
         "Items that do not have parent nodes"));

      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM N where N.I_CLASS=1 and NOT EXISTS "
            + "(select * from JCR_MITEM P where P.I_CLASS=2 and P.PARENT_ID=N.ID "
            + "and P.NAME='[http://www.jcp.org/jcr/1.0]primaryType')"
         : "select * from JCR_SITEM N where N.CONTAINER_NAME='" + jdbcDataContainer.containerName
            + "' and N.I_CLASS=1 and NOT EXISTS (select * from JCR_SITEM P "
            + "where P.I_CLASS=2 and P.PARENT_ID=N.ID and P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' "
            + "and P.CONTAINER_NAME='" + jdbcDataContainer.containerName + "')", new String[]{DBConstants.COLUMN_ID,
         DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
         "Nodes that do not have at least one jcr:primaryType property"));

      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MVALUE V where NOT EXISTS(select * from JCR_MITEM P "
            + "where V.PROPERTY_ID = P.ID and P.I_CLASS=2)"
         : "select * from JCR_SVALUE V where NOT EXISTS(select * from JCR_SITEM P "
            + "where V.PROPERTY_ID = P.ID and P.I_CLASS=2)", new String[]{DBConstants.COLUMN_ID,
         DBConstants.COLUMN_VPROPERTY_ID}, "All value records that has not owner-property record"));

      queries
         .add(new InspectionQueryFilteredMultivaluedProperties(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM P where P.I_CLASS=2 and P.P_MULTIVALUED=? and NOT EXISTS( select * from JCR_MVALUE V "
                  + "where V.PROPERTY_ID=P.ID)"
               : "select * from JCR_SITEM P where P.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "' and P.I_CLASS=2 and P.P_MULTIVALUED=? and NOT EXISTS( select * from JCR_SVALUE V where V.PROPERTY_ID=P.ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "All properties that have not value record."));

      // The differences in the queries by DB dialect.
      // Oracle doesn't work correct with default query because empty value stored as null value.
      String statement;
      if (jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_SYBASE))
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is null and DATA like null) or "
                  + "(STORAGE_DESC is not null and not DATA like null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "'  AND ((STORAGE_DESC is null and DATA like null) or (STORAGE_DESC is not null and not DATA like null))";
      }
      else if (jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLE)
         || jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is not null and DATA is not null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "'  AND (STORAGE_DESC is not null and DATA is not null)";
      }
      else
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is null and DATA is null) or "
                  + "(STORAGE_DESC is not null and DATA is not null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "'  AND ((STORAGE_DESC is null and DATA is null) or (STORAGE_DESC is not null and DATA is not null))";
      }
      queries.add(new InspectionQuery(statement, new String[]{DBConstants.COLUMN_ID}, "Incorrect JCR_VALUE records"));

      queries
         .add(new InspectionQueryFilteredMultivaluedProperties(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM P where P.P_TYPE=9 and P.P_MULTIVALUED=? and NOT EXISTS "
                  + "(select * from JCR_MREF R where P.ID=R.PROPERTY_ID)"
               : "select * from JCR_SITEM P where P.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "' and P.P_TYPE=9 and P.P_MULTIVALUED=? and NOT EXISTS( select * from JCR_SREF R where P.ID=R.PROPERTY_ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "Reference properties without reference records"));

      // an item is its own parent. 
      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM I where I.ID = I.PARENT_ID and I.NAME <> '" + Constants.ROOT_PARENT_NAME + "'"
         : "select * from JCR_SITEM I where I.ID = I.PARENT_ID and I.CONTAINER_NAME='"
            + jdbcDataContainer.containerName + "' and I.NAME <> '" + Constants.ROOT_PARENT_NAME + "'", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME}, "An item is its own parent."));

      // Several versions of same item
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM I where EXISTS (select * from JCR_MITEM J"
                  + " WHERE I.PARENT_ID = J.PARENT_ID AND I.NAME = J.NAME and I.I_INDEX = J.I_INDEX and I.I_CLASS = J.I_CLASS"
                  + " and I.VERSION != J.VERSION)"
               : "select * from JCR_SITEM I where I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "' and"
                  + " EXISTS (select * from JCR_SITEM J WHERE I.CONTAINER_NAME = J.CONTAINER_NAME and"
                  + " I.PARENT_ID = J.PARENT_ID AND I.NAME = J.NAME and I.I_INDEX = J.I_INDEX and I.I_CLASS = J.I_CLASS"
                  + " and I.VERSION != J.VERSION)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME,
               DBConstants.COLUMN_VERSION, DBConstants.COLUMN_CLASS, DBConstants.COLUMN_INDEX},
            "Several versions of same item."));

      // using existing DataSource to get a JDBC Connection.
      Connection jdbcConn = jdbcDataContainer.getConnectionFactory().getJdbcConnection();

      try
      {
         // perform all queries on-by-one
         for (InspectionQuery query : queries)
         {
            PreparedStatement st = null;
            ResultSet resultSet = null;
            try
            {
               st = query.prepareStatement(jdbcConn);

               // the result of query is expected to be empty 
               resultSet = st.executeQuery();
               if (resultSet.next())
               {
                  // but if result not empty, then inconsistency takes place
                  report.logDescription(query.getDescription());
                  do
                  {
                     StringBuilder record = new StringBuilder();
                     for (String fieldName : query.getFieldNames())
                     {
                        record.append(fieldName);
                        record.append('=');
                        if (fieldName.equals(DBConstants.COLUMN_NORDERNUM)
                           || fieldName.equals(DBConstants.COLUMN_VORDERNUM))
                        {
                           record.append(resultSet.getInt(fieldName));
                        }
                        else
                        {
                           record.append(resultSet.getString(fieldName));
                        }
                        record.append(' ');
                     }

                     report.logBrokenObjectAndSetInconsistency(record.toString(), "");
                  }
                  while (resultSet.next());
               }
            }
            // safely free resources
            finally
            {
               if (resultSet != null)
               {
                  try
                  {
                     resultSet.close();
                  }
                  catch (SQLException e)
                  {
                     LOG.error(e.getMessage(), e);
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
                     LOG.error(e.getMessage(), e);
                  }
               }
            }
         }
      }
      catch (SQLException e)
      {
         report.logExceptionAndSetInconsistency("Exception during DB inspection.", e);
      }
      finally
      {
         // safely close connection
         if (jdbcConn != null)
         {
            try
            {
               jdbcConn.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
      }
   }

   /**
    * Inspect ValueStorage. 
    * <p>
    * All ValueDatas that have storage description (that means, value data stored in value storage) will be inspected:
    * <ul>
    * <li> does value exists in value storage;</li>
    * <li> is this value readable;</li>
    * <ul>
    *
    * 
    * @param vsPlugin - value storages
    * @param inspectionLog - log where inspection results will be placed
    * @return resulting InspectionLog
    * @throws RepositoryException
    * @throws IOException
    */
   public static void checkValueStorage(final JDBCWorkspaceDataContainer jdbcDataContainer,
      ValueStoragePluginProvider vsPlugin, InspectionReport report) throws RepositoryException, IOException
   {
      final String valueRecordFormat = "ValueData[PROPERTY_ID=%s ORDER_NUM=%d STORAGE_DESC=%s]";

      Connection connection = jdbcDataContainer.getConnectionFactory().getJdbcConnection();
      PreparedStatement st = null;
      ResultSet resultSet = null;
      try
      {
         st =
            connection.prepareStatement(jdbcDataContainer.multiDb
               ? "SELECT PROPERTY_ID, ORDER_NUM, STORAGE_DESC from JCR_MVALUE where STORAGE_DESC is not null"
               : "SELECT V.PROPERTY_ID, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM I"
                  + " where I.CONTAINER_NAME='" + jdbcDataContainer.containerName
                  + "' and V.PROPERTY_ID = I.ID and STORAGE_DESC is not null");

         resultSet = st.executeQuery();
         // traverse all values, written to value storage
         if (resultSet.next())
         {
            ValueIOChannel channel = null;
            do
            {
               final String propertyId = resultSet.getString(DBConstants.COLUMN_VPROPERTY_ID);
               final int orderNumber = resultSet.getInt(DBConstants.COLUMN_VORDERNUM);
               final String storageDesc = resultSet.getString(DBConstants.COLUMN_VSTORAGE_DESC);

               // don't acquire channel if it is already open 
               if (channel == null || !channel.getStorageId().equals(storageDesc))
               {
                  try
                  {
                     if (channel != null)
                     {
                        channel.close();
                     }
                     channel = vsPlugin.getChannel(storageDesc);
                  }
                  catch (ValueStorageNotFoundException e)
                  {
                     report.logBrokenObjectAndSetInconsistency("ValueStorage " + storageDesc + " not found. "
                        + String.format(valueRecordFormat, propertyId, orderNumber, storageDesc), e.getMessage());
                     continue;
                  }
               }

               try
               {
                  // check value data
                  final ValueIOChannel vdChannel = channel;
                  SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Object>()
                  {
                     public Object run() throws ValueDataNotFoundException, IOException
                     {
                        vdChannel.checkValueData(
                           jdbcDataContainer.multiDb ? propertyId : propertyId
                              .substring(jdbcDataContainer.containerName.length()), orderNumber);
                        return null;
                     }
                  });
               }
               // process exception thrown by checkValueData
               catch (PrivilegedActionException e)
               {
                  Throwable ex = e.getCause();
                  if (ex instanceof ValueDataNotFoundException)
                  {
                     report.logBrokenObjectAndSetInconsistency(
                        String.format(valueRecordFormat, propertyId, orderNumber, storageDesc) + " not found.",
                        ex.getMessage());
                  }
                  else if (ex instanceof IOException)
                  {
                     report.logExceptionAndSetInconsistency(ex.getMessage(), ex);
                  }
                  else
                  {
                     throw new RepositoryException(ex.getMessage(), ex);
                  }
               }
            }
            while (resultSet.next());
         }
      }
      catch (SQLException e)
      {
         report.logExceptionAndSetInconsistency("Exception during ValueStorage inspection.", e);
      }
      finally
      {
         // safely free resources
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
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
               LOG.error(e.getMessage(), e);
            }
         }

         if (connection != null)
         {
            try
            {
               connection.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
      }
   }

}
