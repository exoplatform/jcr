/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.util.jdbc;

import org.exoplatform.services.jcr.impl.util.SecurityHelper;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SingleDBCleaner.java 111 2008-11-11 11:11:11Z serg $
 */
public final class SingleDBCleaner extends DBCleaner
{
   public final String CLEAN_JCR_SITEM_AS_DEFAULT = "/*$CLEAN_JCR_SITEM_DEFAULT*/";

   protected final int MAX_IDS_RETURNED = 100;

   protected String GET_CHILD_IDS;

   protected String REMOVE_ITEMS;

   protected final String containerName;

   public SingleDBCleaner(Connection connection, InputStream inputStream, String containerName) throws IOException
   {
      super(connection, inputStream);
      this.containerName = containerName;
      prepareQueries();
   }

   protected void prepareQueries()
   {
      GET_CHILD_IDS =
         "select ID from JCR_SITEM where CONTAINER_NAME=? and ID not in(select PARENT_ID from JCR_SITEM where CONTAINER_NAME=?)";

      REMOVE_ITEMS = "delete from JCR_SITEM where ID in( ? )";
   }

   /**
    * {@inheritDoc}
    */
   protected boolean canExecuteQuery(Connection conn, String sql) throws SQLException
   {
      if (sql.equalsIgnoreCase(CLEAN_JCR_SITEM_AS_DEFAULT))
      {
         return true;
      }
      else
      {
         return isTablesFromQueryExists(conn, sql);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected void executeQuery(final Statement statement, final String sql) throws SQLException
   {
      if (sql.equalsIgnoreCase(CLEAN_JCR_SITEM_AS_DEFAULT))
      {
         clearItems(statement.getConnection(), containerName);
      }
      else
      {
         // check query for "?" mask and replace it with containerName
         String q = sql.replace("?", "'" + containerName + "'");
         super.executeQuery(statement, q);
      }
   }

   private void clearItems(Connection connection, String containerName) throws SQLException
   {
      // Remove only child nodes in cycle, till all nodes will be removed.
      // Such algorithm used to avoid any constraint violation exception related to foreign key.
      PreparedStatement getChildItems = null;
      final Statement removeItems = connection.createStatement();

      try
      {
         getChildItems = connection.prepareStatement(GET_CHILD_IDS);
         getChildItems.setString(1, containerName);
         getChildItems.setString(2, containerName);

         getChildItems.setMaxRows(MAX_IDS_RETURNED);

         do
         {
            final PreparedStatement getChildIds = getChildItems;
            ResultSet result =
               (ResultSet)SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
               {
                  public Object run() throws Exception
                  {
                     return getChildIds.executeQuery();
                  }
               });

            StringBuilder childListBuilder = new StringBuilder();
            if (result.next())
            {
               childListBuilder.append("'" + result.getString(1) + "'");
            }
            else
            {
               break;
            }
            while (result.next())
            {
               childListBuilder.append(" , '" + result.getString(1) + "'");
            }
            // now remove nodes;
            final String q = REMOVE_ITEMS.replace("?", childListBuilder.toString());
            SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  removeItems.executeUpdate(q);
                  return null;
               }
            });
         }
         while (true);
      }
      finally
      {
         if (getChildItems != null)
         {
            getChildItems.close();
            getChildItems = null;
         }
         if (removeItems != null)
         {
            removeItems.close();
         }
      }
   }
}
