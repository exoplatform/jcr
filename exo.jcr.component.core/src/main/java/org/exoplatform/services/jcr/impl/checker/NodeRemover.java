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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: NodeRemover.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class NodeRemover extends AbstractInconsistencyRepair
{
   private final NodeTypeDataManager nodeTypeManager;

   /**
    * NodeRemover constructor.
    */
   public NodeRemover(WorkspaceStorageConnectionFactory connFactory, NodeTypeDataManager nodeTypeManager)
   {
      super(connFactory);
      this.nodeTypeManager = nodeTypeManager;
   }

   /**
    * {@inheritDoc}
    */
   void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         String parentId = exctractId(resultSet, DBConstants.COLUMN_PARENTID);
         InternalQName nodeName = InternalQName.parse(resultSet.getString(DBConstants.COLUMN_NAME));

         NodeData parent = (NodeData)conn.getItemData(parentId);
         
         NodeDefinitionData def =
            nodeTypeManager.getChildNodeDefinition(nodeName, parent.getPrimaryTypeName(),
               parent.getMixinTypeNames());
         
         if (def == null || def.isResidualSet())
         {
            String nodeId = exctractId(resultSet, DBConstants.COLUMN_ID);
            int orderNum = resultSet.getInt(DBConstants.COLUMN_NORDERNUM);
            int version = resultSet.getInt(DBConstants.COLUMN_VERSION);
            QPath path = QPath.makeChildPath(parent.getQPath(), extractName(resultSet));

            NodeData data =
               new TransientNodeData(path, nodeId, version, null, null, orderNum, Constants.ROOT_UUID, null);

            deleteTree(conn, data);
         }
         else
         {
            throw new SQLException("Node is required by its parent.");
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

   private void deleteTree(JDBCStorageConnection conn, NodeData data) throws IllegalStateException,
      RepositoryException
   {
      for (NodeData child : conn.getChildNodesData(data))
      {
         deleteTree(conn, child);
      }

      for (PropertyData prop : conn.getChildPropertiesData(data))
      {
         conn.delete(prop);
      }

      conn.delete(data);
   }
}
