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
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.checker.AssignerRootAsParent;
import org.exoplatform.services.jcr.impl.checker.DummyRepair;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.checker.InspectionQueryFilteredMultivaluedProperties;
import org.exoplatform.services.jcr.impl.checker.InspectionReport;
import org.exoplatform.services.jcr.impl.checker.RemoverEarlierVersions;
import org.exoplatform.services.jcr.impl.checker.RemoverValueRecords;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandler;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandlerFactory;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 14 жовт. 2011 skarpenko $
 *
 */
public class JDBCWorkspaceDataContainerChecker
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainerChecker");

   protected final JDBCWorkspaceDataContainer jdbcDataContainer;

   protected final ValueStoragePluginProvider vsPlugin;

   protected final WorkspaceEntry workspaceEntry;

   protected final InspectionReport report;

   private InspectionQuery vsInspectionQuery;

   private InspectionQuery lockInspectionQuery;

   private List<InspectionQuery> itemsInspectionQuery = new ArrayList<InspectionQuery>();;

   private LockTableHandler lockHandler;

   /**
    * JDBCWorkspaceDataContainerChecker constructor.
    */
   public JDBCWorkspaceDataContainerChecker(JDBCWorkspaceDataContainer jdbcDataContainer,
      ValueStoragePluginProvider vsPlugin, WorkspaceEntry workspaceEntry, InspectionReport report)
   {
      this.jdbcDataContainer = jdbcDataContainer;
      this.vsPlugin = vsPlugin;
      this.workspaceEntry = workspaceEntry;
      this.report = report;
      this.lockHandler = LockTableHandlerFactory.getHandler(workspaceEntry);

      initInspectionQueries();
   }

   /**
    * Checks jcr locks for consistency. Defines if there is a node with lockIsDeep or lockOwner property 
    * (basically means that the node is to be locked)
    * and has no corresponding record in LockManager persistent layer (db table); 
    * or the opposite.
    */
   public void checkLocksInDataBase(boolean autoRepair)
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      ResultSet resultSet = null;
      PreparedStatement preparedStatement = null;
      Connection jdbcConnection = null;
      try
      {
         jdbcConnection = jdbcDataContainer.getConnectionFactory().getJdbcConnection();
         preparedStatement = lockInspectionQuery.prepareStatement(jdbcConnection);
         resultSet = preparedStatement.executeQuery();

         Set<String> lockedInJCRITEM = new HashSet<String>();
         while (resultSet.next())
         {
            lockedInJCRITEM.add(removeWorkspacePrefix(resultSet.getString(DBConstants.COLUMN_PARENTID)));
         }

         Set<String> lockedInJCRLOCK = lockHandler.getLockedNodesIds();

         checkConsistencyInJCRITEM(lockedInJCRITEM, lockedInJCRLOCK, autoRepair);
         checkConsistencyInJCRLOCK(lockedInJCRITEM, lockedInJCRLOCK, autoRepair);
      }
      catch (SQLException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during LOCK DB checking.", e);
      }
      catch (NamingException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during LOCK DB checking.", e);
      }
      catch (RepositoryConfigurationException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during LOCK DB checking.", e);
      }
      catch (RepositoryException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during LOCK DB checking.", e);
      }
      finally
      {
         JDBCUtils.freeResources(resultSet, preparedStatement, jdbcConnection);
      }
   }

   private void checkConsistencyInJCRITEM(Set<String> lockedInJCRITEM, Set<String> lockedInJCRLOCK,
      boolean autoRepair) throws RepositoryException, SQLException
   {
      for (String nodeId : lockedInJCRITEM)
      {
         if (!lockedInJCRLOCK.contains(nodeId))
         {
            logBrokenObjectAndSetInconsistency("Lock exists in ITEM table but not in LOCK table. Node UUID: "
               + nodeId);
            
            if (autoRepair)
            {
               WorkspaceStorageConnection conn = jdbcDataContainer.openConnection();
               try
               {
                  if (conn instanceof JDBCStorageConnection)
                  {
                     ((JDBCStorageConnection)conn).deleteLockProperties(nodeId);
                  }

                  logComment("Lock has been removed form ITEM table. Node UUID: " + nodeId);
               }
               finally
               {
                  conn.close();
               }
            }
         }
      }
   }

   private void checkConsistencyInJCRLOCK(Set<String> lockedInJCRITEM, Set<String> lockedInJCRLOCK, boolean autoRepair)
      throws NamingException, RepositoryConfigurationException, SQLException
   {
      for (String nodeId : lockedInJCRLOCK)
      {
         if (!lockedInJCRITEM.contains(nodeId))
         {
            logBrokenObjectAndSetInconsistency("Lock exists in LOCK table but not in ITEM table. Node UUID: " + nodeId);

            if (autoRepair)
            {
               lockHandler.removeLockedNode(nodeId);
               logComment("Lock has been removed form LOCK table. Node UUID: " + nodeId);
            }
         }
      }
   }

   /**
    * Check database.
    * <p>
    * Check that database is not broken, and all base relation between jcr-items are not corrupted.
    * </p>
    */
   public void checkDataBase(boolean autoRepair)
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      
      Connection jdbcConn = null;
      try
      {
         jdbcConn = jdbcDataContainer.getConnectionFactory().getJdbcConnection();

         for (InspectionQuery query : itemsInspectionQuery)
         {
            PreparedStatement st = null;
            ResultSet resultSet = null;
            try
            {
               st = query.prepareStatement(jdbcConn);

               resultSet = st.executeQuery();
               if (resultSet.next())
               {
                  logDescription(query.getDescription());
                  do
                  {
                     logBrokenObjectAndSetInconsistency(getBrokenObject(resultSet, query.getFieldNames()));
                     if (autoRepair)
                     {
                        query.getRepair().doRepair(resultSet);
                        logComment("Inconsistency has been fixed");
                     }
                  }
                  while (resultSet.next());
               }
            }
            finally
            {
               JDBCUtils.freeResources(resultSet, st, null);
            }
         }
      }
      catch (SQLException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during DB checking.", e);
      }
      catch (RepositoryException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during DB checking.", e);
      }
      finally
      {
         JDBCUtils.freeResources(null, null, jdbcConn);
      }
   }

   /**
    * Inspect ValueStorage.
    * <p>
    * All ValueDatas that have storage description (that means, value data stored in value storage) will be inspected:
    * <ul>
    * <li> does value exists in value storage;</li>
    * <ul>
    */
   public void checkValueStorage(boolean autoRepair)
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      Connection connection = null;
      PreparedStatement st = null;
      ResultSet resultSet = null;
      try
      {
         connection = jdbcDataContainer.getConnectionFactory().getJdbcConnection();
         st = vsInspectionQuery.prepareStatement(connection);
         resultSet = st.executeQuery();

         if (resultSet.next())
         {
            do
            {
               String propertyId = removeWorkspacePrefix(resultSet.getString(DBConstants.COLUMN_VPROPERTY_ID));
               int orderNumber = resultSet.getInt(DBConstants.COLUMN_VORDERNUM);
               String storageDesc = resultSet.getString(DBConstants.COLUMN_VSTORAGE_DESC);

               ValueIOChannel channel = null;
               try
               {
                  channel = getIOChannel(storageDesc);
                  doCheckAndRepairValueData(channel, resultSet, propertyId, orderNumber, autoRepair);
               }
               catch (IOException e)
               {
                  logDescription("Unexpected exception during checking");
                  logBrokenObjectAndSetInconsistency(getBrokenObject(resultSet, vsInspectionQuery.getFieldNames()));
                  logExceptionAndSetInconsistency(e.getMessage(), e);
               }
               finally
               {
                  if (channel != null)
                  {
                     channel.close();
                  }
               }
            }
            while (resultSet.next());
         }
      }
      catch (SQLException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during checking.", e);
      }
      catch (RepositoryException e)
      {
         logExceptionAndSetInconsistency("Unexpected exception during checking.", e);
      }
      finally
      {
         JDBCUtils.freeResources(resultSet, st, connection);
      }
   }

   private ValueIOChannel getIOChannel(String storageDesc) throws IOException
   {
      ValueIOChannel channel = null;
      try
      {
         channel = vsPlugin.getChannel(storageDesc);
      }
      catch (ValueStorageNotFoundException e)
      {
         logDescription("ValueStorage " + storageDesc + " not found");
         logExceptionAndSetInconsistency(e.getMessage(), e);
      }

      return channel;
   }

   private void doCheckAndRepairValueData(ValueIOChannel channel, ResultSet resultSet, String propertyId,
      int orderNumber, boolean autoRepair) throws IOException
   {
      try
      {
         channel.checkValueData(propertyId, orderNumber);
      }
      catch (ValueDataNotFoundException e)
      {
         String brokenObject = getBrokenObject(resultSet, vsInspectionQuery.getFieldNames());

         logDescription("ValueData not found");
         logBrokenObjectAndSetInconsistency(brokenObject);
         logExceptionAndSetInconsistency(e.getMessage(), e);

         if (autoRepair)
         {
            channel.repairValueData(propertyId, orderNumber);
            logComment("ValueData corresponding to " + brokenObject + " has been repaired. New empty file is created.");
         }
      }
   }

   private String removeWorkspacePrefix(String str)
   {
      return jdbcDataContainer.multiDb ? str : str.substring(jdbcDataContainer.containerName.length());
   }

   private String getBrokenObject(ResultSet resultSet, String[] fieldNames)
   {
      StringBuilder record = new StringBuilder();
      for (String fieldName : fieldNames)
      {
         record.append(fieldName);
         record.append('=');

         try
         {
            record.append(resultSet.getString(fieldName));
         }
         catch (SQLException e)
         {
            LOG.error(e.getMessage(), e);
         }
         record.append(' ');
      }
      
      return record.toString();
   }

   private void logBrokenObjectAndSetInconsistency(String brokenObject)
   {
      try
      {
         report.logBrokenObjectAndSetInconsistency(brokenObject);
      }
      catch (IOException e)
      {
         LOG.error(e.getMessage(), e);
      }
   }

   private void logComment(String message)
   {
      try
      {
         report.logComment(message);
      }
      catch (IOException e1)
      {
         LOG.error(e1.getMessage(), e1);
      }
   }

   private void logDescription(String description)
   {
      try
      {
         report.logDescription(description);
      }
      catch (IOException e1)
      {
         LOG.error(e1.getMessage(), e1);
      }
   }

   private void logExceptionAndSetInconsistency(String message, Throwable e)
   {
      try
      {
         report.logExceptionAndSetInconsistency(message, e);
      }
      catch (IOException e1)
      {
         LOG.error(e1.getMessage(), e1);
      }
   }

   private void initInspectionQueries()
   {
      vsInspectionQuery =
         new InspectionQuery(jdbcDataContainer.multiDb
            ? "select PROPERTY_ID, ORDER_NUM, STORAGE_DESC from JCR_MVALUE where STORAGE_DESC is not null"
            : "select V.PROPERTY_ID, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM I"
               + " where I.CONTAINER_NAME='" + jdbcDataContainer.containerName
               + "' and V.PROPERTY_ID = I.ID and STORAGE_DESC is not null",
            new String[]{DBConstants.COLUMN_VPROPERTY_ID, DBConstants.COLUMN_VORDERNUM,
               DBConstants.COLUMN_VSTORAGE_DESC}, "Items with value data stored in value storage", new DummyRepair());

      lockInspectionQuery =
         new InspectionQuery(jdbcDataContainer.multiDb ? "select distinct PARENT_ID from JCR_MITEM where I_CLASS=2 AND"
            + " (NAME='[http://www.jcp.org/jcr/1.0]lockOwner' OR NAME='[http://www.jcp.org/jcr/1.0]lockIsDeep')"
            : "select distinct PARENT_ID from JCR_SITEM WHERE CONTAINER_NAME='" + jdbcDataContainer.containerName + "'"
               + " AND I_CLASS=2 and (NAME='[http://www.jcp.org/jcr/1.0]lockOwner'"
               + " OR NAME='[http://www.jcp.org/jcr/1.0]lockIsDeep')", new String[]{DBConstants.COLUMN_PARENTID},
            "Items which have jcr:lockOwner and jcr:lockIsDeep properties", new DummyRepair());

      // ITEM tables
      itemsInspectionQuery.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM I where NOT EXISTS(select * from JCR_MITEM P where P.ID = I.PARENT_ID)"
         : "select * from JCR_SITEM I where I.CONTAINER_NAME='" + jdbcDataContainer.containerName
            + "' and NOT EXISTS(select * from JCR_SITEM P where P.ID = I.PARENT_ID)", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME, DBConstants.COLUMN_CLASS},
            "Items that do not have parent nodes", new AssignerRootAsParent(jdbcDataContainer
               .getConnectionFactory())));

      itemsInspectionQuery
         .add(new InspectionQueryFilteredMultivaluedProperties(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM P where P.I_CLASS=2 and P.P_MULTIVALUED=? and NOT EXISTS( select * from JCR_MVALUE V "
                  + "where V.PROPERTY_ID=P.ID)" : "select * from JCR_SITEM P where P.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "' and P.I_CLASS=2"
                  + " and P.P_MULTIVALUED=? and NOT EXISTS( select * from JCR_SVALUE V where V.PROPERTY_ID=P.ID)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
            "A node that has a single valued properties with nothing declared in the VALUE table.", new DummyRepair()));

      itemsInspectionQuery.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM N where N.I_CLASS=1 and NOT EXISTS "
            + "(select * from JCR_MITEM P where P.I_CLASS=2 and P.PARENT_ID=N.ID "
            + "and P.NAME='[http://www.jcp.org/jcr/1.0]primaryType')"
         : "select * from JCR_SITEM N where N.CONTAINER_NAME='" + jdbcDataContainer.containerName
            + "' and N.I_CLASS=1 and NOT EXISTS (select * from JCR_SITEM P "
            + "where P.I_CLASS=2 and P.PARENT_ID=N.ID and P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' "
            + "and P.CONTAINER_NAME='" + jdbcDataContainer.containerName + "')", new String[]{DBConstants.COLUMN_ID,
         DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME}, "A node that doesn't have primary type property",
         new DummyRepair()));

      itemsInspectionQuery.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MVALUE V where NOT EXISTS(select * from JCR_MITEM P "
            + "where V.PROPERTY_ID = P.ID and P.I_CLASS=2)"
         : "select * from JCR_SVALUE V where NOT EXISTS(select * from JCR_SITEM P "
            + "where V.PROPERTY_ID = P.ID and P.I_CLASS=2)", new String[]{DBConstants.COLUMN_ID,
         DBConstants.COLUMN_VPROPERTY_ID}, "All value records that has not related property record",
         new RemoverValueRecords(jdbcDataContainer.getConnectionFactory(), jdbcDataContainer.containerName,
            jdbcDataContainer.multiDb)));

      // The differences in the queries by DB dialect.
      String statement;
      if (jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_SYBASE))
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is not null and not DATA like null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "' AND ((STORAGE_DESC is not null and not DATA like null))";
      }
      else if (jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLE)
         || jdbcDataContainer.dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is not null and DATA is not null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "' AND (STORAGE_DESC is not null and DATA is not null)";
      }
      else
      {
         statement =
            jdbcDataContainer.multiDb
               ? "select * from JCR_MVALUE where (STORAGE_DESC is not null and DATA is not null)"
               : "select V.* from JCR_SVALUE V, JCR_SITEM I where V.PROPERTY_ID = I.ID and I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName + "' AND ((STORAGE_DESC is not null and DATA is not null))";
      }
      itemsInspectionQuery.add(new InspectionQuery(statement, new String[]{DBConstants.COLUMN_ID},
         "Incorrect VALUE records. Both fields STORAGE_DESC and DATA contain not null value.", new DummyRepair()));

      itemsInspectionQuery.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM I where I.ID = I.PARENT_ID and I.NAME <> '" + Constants.ROOT_PARENT_NAME + "'"
         : "select * from JCR_SITEM I where I.ID = I.PARENT_ID and I.CONTAINER_NAME='"
            + jdbcDataContainer.containerName + "' and I.NAME <> '" + Constants.ROOT_PARENT_NAME + "'", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME}, "An item is its own parent.",
         new AssignerRootAsParent(jdbcDataContainer.getConnectionFactory())));

      itemsInspectionQuery
         .add(new InspectionQuery(
            jdbcDataContainer.multiDb
               ? "select * from JCR_MITEM I where EXISTS (select * from JCR_MITEM J"
                  + " WHERE I.PARENT_ID = J.PARENT_ID AND I.NAME = J.NAME and I.I_INDEX = J.I_INDEX and I.I_CLASS = J.I_CLASS"
                  + " and I.VERSION != J.VERSION)"
               : "select * from JCR_SITEM I where I.CONTAINER_NAME='"
                  + jdbcDataContainer.containerName
                  + "' and EXISTS (select * from JCR_SITEM J WHERE I.CONTAINER_NAME = J.CONTAINER_NAME and"
                  + " I.PARENT_ID = J.PARENT_ID AND I.NAME = J.NAME and I.I_INDEX = J.I_INDEX and I.I_CLASS = J.I_CLASS"
                  + " and I.VERSION != J.VERSION)",
            new String[]{DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME,
               DBConstants.COLUMN_VERSION, DBConstants.COLUMN_CLASS, DBConstants.COLUMN_INDEX},
            "Several versions of same item.", new RemoverEarlierVersions(jdbcDataContainer.getConnectionFactory())));

      itemsInspectionQuery.add(new InspectionQuery(jdbcDataContainer.multiDb
         ? "select * from JCR_MITEM P, JCR_MVALUE V where P.ID=V.PROPERTY_ID and P.P_TYPE=9 and NOT EXISTS "
            + "(select * from JCR_MREF R where P.ID=R.PROPERTY_ID)"
         : "select * from JCR_SITEM P, JCR_SVALUE V where P.ID=V.PROPERTY_ID and P.CONTAINER_NAME='"
            + jdbcDataContainer.containerName
            + "' and P.P_TYPE=9 and NOT EXISTS (select * from JCR_SREF R where P.ID=R.PROPERTY_ID)", new String[]{
         DBConstants.COLUMN_ID, DBConstants.COLUMN_PARENTID, DBConstants.COLUMN_NAME},
         "Reference properties without reference records", new DummyRepair()));
   }
}
