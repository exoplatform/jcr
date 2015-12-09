/*
 * Copyright (C) 2015 eXo Platform SAS.
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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.jcr.RepositoryException;


/**
 * @author <a href="aboughzela@exoplatform.com">Aymen Boughzela</a>
 * @version $Id: ReferencePropertyRemover.java
 */
public class ReferencePropertyRemover extends AbstractInconsistencyRepair
{
   private final NodeTypeDataManager nodeTypeManager;

   /**
    * ReferencePropertyRemover constructor.
    * @param connFactory
    * @param containerConfig
    */
   public ReferencePropertyRemover(WorkspaceStorageConnectionFactory connFactory, JDBCDataContainerConfig containerConfig, NodeTypeDataManager nodeTypeManager)
   {
      super(connFactory, containerConfig);
      this.nodeTypeManager = nodeTypeManager;
   }

   @Override
   void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         PropertyData data = (PropertyData)conn.getItemData(getIdentifier(resultSet, DBConstants.COLUMN_ID));

         boolean multiValued = resultSet.getBoolean(DBConstants.COLUMN_PMULTIVALUED);
         int version = resultSet.getInt(DBConstants.COLUMN_VERSION);
         int type = resultSet.getInt(DBConstants.COLUMN_PTYPE);
         String parentId = getIdentifier(resultSet, DBConstants.COLUMN_PARENTID);
         String propertyId = getIdentifier(resultSet, DBConstants.COLUMN_ID);
         InternalQName propertyName = InternalQName.parse(resultSet.getString(DBConstants.COLUMN_NAME));

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
            if (LOG.isTraceEnabled())
            {
               LOG.trace(e.getMessage(), e);
            }
         }
         if (def == null || def.getDefinition(multiValued) == null || def.getDefinition(multiValued).isResidualSet()
            || (!def.getDefinition(multiValued).isMandatory()))
         {
            if (def.getDefinition(multiValued).isMultiple())
            {
               String vData;
               ArrayList<ValueData> list = new ArrayList<ValueData>();
               for (ValueData v : data.getValues())
               {
                  vData = new String(v.getAsByteArray(), Constants.DEFAULT_ENCODING);
                  if (vData != null && !vData.isEmpty())
                  {
                     list.add(v);
                  }
               }
               if (list.isEmpty())
               {
                  conn.delete(data, new SimpleChangedSizeHandler());
               }
               else
               {
                  QPath path = new QPath(new QPathEntry[]{getQPathEntry(resultSet)});

                  PropertyData newData =
                     new TransientPropertyData(path, propertyId, version, type, parentId, true, list);
                  conn.update(newData, new SimpleChangedSizeHandler());
               }
            }

            else
            {
               conn.delete(data, new SimpleChangedSizeHandler());

            }
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
      catch (UnsupportedEncodingException e)
      {
         throw new SQLException(e);
      }
      catch (IOException e)
      {
         throw new SQLException(e);
      }
   }
}
