/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.checker;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: NodeRemover.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class NodeRemover extends AbstractInconsistencyRepair
{
   private final NodeTypeDataManager nodeTypeManager;

   /**
    * JCR item table name.
    */
   private final String iTable;

   /**
    * NodeRemover constructor.
    */
   public NodeRemover(WorkspaceStorageConnectionFactory connFactory, String iTable, NodeTypeDataManager nodeTypeManager)
   {
      super(connFactory);

      this.nodeTypeManager = nodeTypeManager;
      this.iTable = iTable;
   }

   /**
    * {@inheritDoc}
    */
   void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         validateIfRequiredByParent(conn, resultSet);

         removeChildrenItems(conn, resultSet);

         NodeData data = createNodeData(resultSet);
         conn.delete(data);
      }
      catch (JCRInvalidItemStateException e)
      {
         // It is ok. Node already removed in previous check
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }
      catch (IllegalStateException e)
      {
         throw new SQLException(e);
      }
      catch (RepositoryException e)
      {
         throw new SQLException(e);
      }
      catch (IllegalNameException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * Validates if node represented by instance of {@link ResultSet} is mandatory 
    * for parent node. It means should not be removed. Throws {@link SQLException}
    * in this case with appropriate message.
    */
   private void validateIfRequiredByParent(JDBCStorageConnection conn, ResultSet resultSet) throws RepositoryException,
      SQLException, IllegalNameException
   {
      String parentId = exctractId(resultSet, DBConstants.COLUMN_PARENTID);
      InternalQName nodeName = InternalQName.parse(resultSet.getString(DBConstants.COLUMN_NAME));

      NodeData parent = null;
      try
      {
         parent = (NodeData)conn.getItemData(parentId);
      }
      catch (PrimaryTypeNotFoundException e)
      {
         // It is possible, parent also without primaryType property
         return;
      }

      // parent already removed in previous check
      if (parent == null)
      {
         return;
      }

      NodeDefinitionData def =
         nodeTypeManager.getChildNodeDefinition(nodeName, parent.getPrimaryTypeName(), parent.getMixinTypeNames());

      if (!def.isResidualSet())
      {
         throw new SQLException("Node is required by its parent.");
      }
   }

   /**
    * Removes all children items. 
    */
   private void removeChildrenItems(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException,
      IllegalNameException, IllegalStateException, UnsupportedOperationException, InvalidItemStateException,
      RepositoryException
   {
      String parentId = resultSet.getString(DBConstants.COLUMN_ID);
      String selectStatement = "select * from " + iTable + " where I_CLASS = 1 and PARENT_ID = '" + parentId + "'";
      String deleteStatement = "delete from " + iTable + " where I_CLASS = 1 and PARENT_ID = '" + parentId + "'";

      // traversing down to the bottom of the tree
      PreparedStatement statement = conn.getJdbcConnection().prepareStatement(selectStatement);
      ResultSet selResult = statement.executeQuery();
      try
      {
         while (selResult.next())
         {
            removeChildrenItems(conn, selResult);
         }
      }
      finally
      {
         JDBCUtils.freeResources(selResult, statement, null);
      }

      // remove properties
      NodeData node = createNodeData(resultSet);
      for (PropertyData prop : conn.getChildPropertiesData(node))
      {
         conn.delete(prop);
      }

      // remove nodes
      statement = conn.getJdbcConnection().prepareStatement(deleteStatement);
      try
      {
         statement.execute();
      }
      finally
      {
         JDBCUtils.freeResources(null, statement, null);
      }
   }

   /**
    * Restore {@link NodeData} represented by row in ITEM table.
    */
   private NodeData createNodeData(ResultSet resultSet) throws SQLException, IllegalPathException, IllegalNameException
   {
      String nodeId = exctractId(resultSet, DBConstants.COLUMN_ID);
      int orderNum = resultSet.getInt(DBConstants.COLUMN_NORDERNUM);
      int version = resultSet.getInt(DBConstants.COLUMN_VERSION);
      QPath path = QPath.makeChildPath(QPath.parse("[]unknown-parent-node-remover"), extractName(resultSet));

      return new TransientNodeData(path, nodeId, version, Constants.NT_UNSTRUCTURED, new InternalQName[0], orderNum,
         Constants.ROOT_UUID, new AccessControlList());
   }
}
