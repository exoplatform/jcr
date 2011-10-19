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
import org.exoplatform.services.jcr.impl.InspectionLog;
import org.exoplatform.services.jcr.impl.InspectionLog.InspectionStatus;
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
   public static void checkDB(JDBCWorkspaceDataContainer jdbcDataContainer, InspectionLog inspectionLog)
      throws RepositoryException, IOException
   {

      /**
       * Data class, contains a combination of SQL states, description, field names and status  
       */
      class InspectionQuery
      {
         /**
          * SQL query that must be executed.
          */
         private final String statement;

         /**
          * Inspection query description.
          */
         private final String description;

         /**
          * Field names that must be showed in inspection log if something wrong.
          */
         private final String[] fieldNames;

         /**
          * Corruption status. Is it critical - <b>ERR</b>, or not - <b>WARN</b>.
          */
         private final InspectionStatus status;

         public InspectionQuery(String statement, String[] fieldNames, String headerMessage, InspectionStatus status)
         {
            this.statement = statement;
            this.description = headerMessage;
            this.fieldNames = fieldNames;
            this.status = status;
         }

         public String getStatement()
         {
            return statement;
         }

         public String getDescription()
         {
            return description;
         }

         public String[] getFieldNames()
         {
            return fieldNames;
         }

         public InspectionStatus getStatus()
         {
            return status;
         }
      }

      Set<InspectionQuery> queries = new HashSet<InspectionQuery>();

      // preload queries
      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM as I where NOT EXISTS(select * from JCR_MITEM AS P where P.ID = I.PARENT_ID)"
         : "select * from JCR_SITEM as I where I.CONTAINER_NAME='" + jdbcDataContainer.containerName
            + "' and NOT EXISTS(select * from JCR_SITEM AS P where P.ID = I.PARENT_ID)", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME, DBConstants.COLUMN_CLASS},
         "Items that do not have parent nodes", InspectionStatus.ERR));
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM as N where N.I_CLASS=1 and NOT EXISTS (select * from JCR_MITEM AS P where P.I_CLASS=2 and P.PARENT_ID=N.ID)"
               : "select * from JCR_SITEM as N where N.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "' and N.I_CLASS=1 and NOT EXISTS (select * from JCR_SITEM AS P where P.I_CLASS=2 and P.PARENT_ID=N.ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "Nodes that do not have at least one property", InspectionStatus.ERR));
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE as V where NOT EXISTS(select * from JCR_MITEM as P where V.PROPERTY_ID = P.ID and P.I_CLASS=2)"
               : "select * from JCR_SVALUE as V where NOT EXISTS(select * from JCR_SITEM as P where P.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "' and V.PROPERTY_ID = P.ID and P.I_CLASS=2)", new String[]{
               DBConstants.COLUMN_ID, DBConstants.COLUMN_VPROPERTY_ID},
            "All value records that has not owner-property record", InspectionStatus.ERR));
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM as P where P.I_CLASS=2 and NOT EXISTS( select * from JCR_MVALUE as V where V.PROPERTY_ID=P.ID)"
               : "select * from JCR_SITEM as P where P.CONTAINER_NAME='" + jdbcDataContainer.containerName
                  + "' and P.I_CLASS=2 and NOT EXISTS( select * from JCR_SVALUE as V where V.PROPERTY_ID=P.ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "All properties that have not value record.", InspectionStatus.WARN));
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is null and DATA is null) or (STORAGE_DESC is not null and DATA is not null)"
               : "select * from JCR_SVALUE where (STORAGE_DESC is null and DATA is null) or (STORAGE_DESC is not null and DATA is not null)",
            new String[]{DBConstants.COLUMN_ID}, "Incorrect JCR_VALUE records", InspectionStatus.ERR));
      queries
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM AS P where P.P_TYPE=9 and NOT EXISTS( select * from JCR_MREF AS R where P.ID=R.PROPERTY_ID)"
               : "select * from JCR_SITEM AS P where P.CONTAINER_NAME='" + jdbcDataContainer.containerName
                  + "' and P.P_TYPE=9 and NOT EXISTS( select * from JCR_SREF AS R where P.ID=R.PROPERTY_ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "Reference properties without reference records", InspectionStatus.ERR));

      // properties can refer to missing node. It is possible to perform this usecase via JCR API with no exceptions 
      queries.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MREF AS R where NOT EXISTS(select * from JCR_MITEM AS N where R.NODE_ID=N.ID)"
         : "select * from JCR_SREF AS R where NOT EXISTS(select * from JCR_SITEM AS N where N.CONTAINER_NAME='"
            + jdbcDataContainer.containerName + "' and R.NODE_ID=N.ID)", new String[]{"NODE_ID", "PROPERTY_ID",
         DBConstants.COLUMN_VORDERNUM},
         "Reference records that linked to unexisted nodes. Can be normal for some usecases.", InspectionStatus.WARN));

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
               st = jdbcConn.prepareStatement(query.getStatement());
               // the result of query is expected to be empty
               resultSet = st.executeQuery();
               if (resultSet.next())
               {
                  // but if result not empty, then inconsistency takes place
                  inspectionLog.logInspectionDescription(query.getDescription());
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
                     // log inconsistency issue.
                     inspectionLog.logBrokenObjectInfo(record.toString(), "", query.getStatus());
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
         // log unexpected exceptions to log
         inspectionLog.logException("Exception during DB inspection.", e);
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
   public static void checkValueStorage(JDBCWorkspaceDataContainer jdbcDataContainer,
      ValueStoragePluginProvider vsPlugin, InspectionLog inspectionLog) throws RepositoryException, IOException
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
               : "SELECT PROPERTY_ID, ORDER_NUM, STORAGE_DESC from JCR_SVALUE where STORAGE_DESC is not null");

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
                     inspectionLog.logBrokenObjectInfo("ValueStorage " + storageDesc + " not found. "
                        + String.format(valueRecordFormat, propertyId, orderNumber, storageDesc), e.getMessage(),
                        InspectionStatus.ERR);
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
                        vdChannel.checkValueData(propertyId, orderNumber);
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
                     inspectionLog.logBrokenObjectInfo(String.format(valueRecordFormat, propertyId, orderNumber,
                        storageDesc)
                        + " not found.", ex.getMessage(), InspectionStatus.ERR);
                  }
                  else if (ex instanceof IOException)
                  {
                     inspectionLog.logException(ex.getMessage(), (IOException)ex);
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
         // log unexpceted exception
         inspectionLog.logException("Exception during ValueStorage inspection.", e);
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
