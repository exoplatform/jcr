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
package org.exoplatform.services.jcr.impl.util.jdbc.cleaner;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBCleanHelper
{

   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleanHelper");

   /**
    * SELECT_ITEMS.
    */
   private final String SELECT_ITEMS = "select ID from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

   /**
    * REMOVE_ITEMS.
    */
   private final String REMOVE_ITEMS = "delete from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

   /**
    * Container name.
    */
   protected final String containerName;

   /**
    * Connection to database.
    */
   protected final Connection connection;

   /**
    * DBCleanerHelper constructor.
    */
   public DBCleanHelper(String containerName, Connection connection)
   {
      this.connection = connection;
      this.containerName = containerName;
   }

   /**
    * Removing rows from JCR_SITEM table. Some database do not support cascade delete, 
    * or need special sittings, so query "delete from JCR_SITEM where CONTAINER_NAME=?" 
    * may cause constraint violation exception. In such case will be used deleting like
    * visitor does. First traverse to the bottom of the tree and then go up to the root
    * and perform deleting children.
    * 
    * @throws SQLException 
    *          SQL exception. 
    */
   public void clean() throws DBCleanerException
   {
      try
      {
         connection.setAutoCommit(false);

         recursiveClean(Constants.ROOT_PARENT_UUID);

         connection.commit();
      }
      catch (SQLException e)
      {
         try
         {
            connection.rollback();
         }
         catch (SQLException rollbackException)
         {
            LOG.error("Can not rollback changes after exception " + e.getMessage(), rollbackException);
         }
         throw new DBCleanerException(e.getMessage(), e);
      }
   }

   private void recursiveClean(String parentID) throws SQLException
   {
      PreparedStatement selectItems = null;
      PreparedStatement removeItems = null;
      ResultSet result = null;

      try
      {
         selectItems = connection.prepareStatement(SELECT_ITEMS);
         selectItems.setString(1, containerName);
         selectItems.setString(2, parentID);

         final PreparedStatement selectStatement = selectItems;
         result = (ResultSet)SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
         {
            public Object run() throws Exception
            {
               return selectStatement.executeQuery();
            }
         });

         // recursive traversing to the bottom of the tree
         if (result.next())
         {
            do
            {
               recursiveClean(result.getString(1));
            }
            while (result.next());

            // go up to the root and remove all nodes
            removeItems = connection.prepareStatement(REMOVE_ITEMS);
            removeItems.setString(1, containerName);
            removeItems.setString(2, parentID);

            final PreparedStatement deleteStatement = removeItems;
            SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  deleteStatement.executeUpdate();
                  return null;
               }
            });
         }
      }
      finally
      {
         if (selectItems != null)
         {
            selectItems.close();
         }

         if (removeItems != null)
         {
            removeItems.close();
         }

         if (result != null)
         {
            result.close();
         }
      }
   }
}
