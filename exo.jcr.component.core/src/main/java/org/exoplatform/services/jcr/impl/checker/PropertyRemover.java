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

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: PropertyRemover.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class PropertyRemover extends AbstractInconsistencyRepair
{
   private final NodeTypeDataManager nodeTypeManager;

   /**
    * PropertyRemover constructor.
    */
   public PropertyRemover(WorkspaceStorageConnectionFactory connFactory, NodeTypeDataManager nodeTypeManager)
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
         InternalQName propertyName = InternalQName.parse(resultSet.getString(DBConstants.COLUMN_NAME));
         boolean multiValued = resultSet.getBoolean(DBConstants.COLUMN_PMULTIVALUED);

         PropertyDefinitionDatas def = null;
         try
         {
            NodeData parent = (NodeData)conn.getItemData(parentId);
            def =
               nodeTypeManager.getPropertyDefinitions(propertyName, parent.getPrimaryTypeName(),
                  parent.getMixinTypeNames());
         }
         catch (PrimaryTypeNotFoundException e)
         {
            // It is possible.
         }
         
         if (def == null || def.getDefinition(multiValued) == null || def.getDefinition(multiValued).isResidualSet())
         {
            String propertyId = exctractId(resultSet, DBConstants.COLUMN_ID);
            QPath path = new QPath(new QPathEntry[]{extractName(resultSet)});

            PropertyData data =
               new TransientPropertyData(path, propertyId, 0, 0, null, false, new ArrayList<ValueData>());

            conn.delete(data);
         }
         else
         {
            throw new SQLException("Propety is required by its parent.");
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
}
