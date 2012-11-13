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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 20.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class MySQLMultiDbJDBCConnection extends MultiDbJDBCConnection
{
   /**
    * Keeping identifiers of deleted nodes in memory for improving performance
    * and avoiding issue with batching update.
    */
   protected final Set<String> addedNodes = new HashSet<String>();

   /**
    * Indicates if we have deal with MySQL innoDB engine, which supports foreign keys.
    */
   protected final boolean innoDBEngine;

   protected String PATTERN_ESCAPE_STRING = "\\\\";

   /**
    * MySQL Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public MySQLMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);

      this.innoDBEngine =
         containerConfig.equals(DBConstants.DB_DIALECT_MYSQL)
            || containerConfig.equals(DBConstants.DB_DIALECT_MYSQL_UTF8);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {
      super.prepareQueries();

      FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ =
         FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ.replace("from " + JCR_ITEM + " I, " + JCR_VALUE + " V", "from "
            + JCR_ITEM + " I force index (" + JCR_IDX_ITEM_PARENT_NAME + "),  " + JCR_VALUE + " V force index ("
            + JCR_IDX_VALUE_PROPERTY + ")");

      FIND_NODES_AND_PROPERTIES =
         FIND_NODES_AND_PROPERTIES.replace("from " + JCR_ITEM + " I", "from " + JCR_ITEM + " I force index (PRIMARY)");

      FIND_ITEM_BY_NAME = "select * from " + JCR_ITEM + " where PARENT_ID=? and NAME=? and I_INDEX=? order by I_CLASS";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException, InvalidItemStateException, RepositoryException
   {
      // check if parent exists
      if (isParentValidationNeeded(data.getParentIdentifier()))
      {
         ResultSet item = findItemByIdentifier(data.getParentIdentifier());
         try
         {
            if (!item.next())
            {
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
            }
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }

      if (!innoDBEngine)
      {
         addedNodes.add(data.getIdentifier());
      }
      return super.addNodeRecord(data);
   }

   /**
    * {@inheritDoc}
    */
   public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      if (!innoDBEngine)
      {
         addedNodes.remove(data.getIdentifier());
      }

      super.delete(data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException, InvalidItemStateException,
      RepositoryException
   {
      // check if parent exists
      if (isParentValidationNeeded(data.getParentIdentifier()))
      {
         ResultSet item = findItemByIdentifier(data.getParentIdentifier());
         try
         {
            if (!item.next())
            {
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
            }
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
      return super.addPropertyRecord(data);
   }

   @Override
   protected String getLikeExpressionEscape()
   {
      // must be .. LIKE 'prop\\_name' ESCAPE '\\\\'
      return this.PATTERN_ESCAPE_STRING;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IllegalStateException, RepositoryException
   {
      if (!innoDBEngine)
      {
         addedNodes.clear();
      }

      super.close();
   }

   /**
    * Returns true if parent validation is needed. Some MySQL engines does not support
    * foreign keys, such as MyISAM or NDB, that is why it is needed to execute additional
    * query. 
    */
   protected boolean isParentValidationNeeded(String parentIdentifier)
   {
      return !innoDBEngine && parentIdentifier != null && !addedNodes.contains(parentIdentifier);
   }
}
