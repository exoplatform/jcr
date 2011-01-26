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
package org.exoplatform.services.jcr.impl.clean.rdbms;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 
 * Created by The eXo Platform SAS.
 *
 * Date: 21.01.2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DBCleanHelper.java.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DBCleanHelper
{

   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleanHelper");

   /**
    * Select items query.
    */
   private final String selectItems;

   /**
    * Remove items query.
    */
   private final String removeItems;

   /**
    * Connection to database.
    */
   protected final Connection connection;

   /**
    * DBCleanerHelper constructor.
    */
   public DBCleanHelper(Connection connection, String selectItemds, String removeItems)
   {
      this.connection = connection;
      this.selectItems = selectItemds;
      this.removeItems = removeItems;
   }

   /**
    * Removing rows from table. Some database do not support cascade delete, 
    * or need special sittings. In such case will be used deleting like
    * visitor does. First traverse to the bottom of the tree and then go up to the root
    * and perform deleting children.
    * 
    * @throws SQLException 
    *          SQL exception. 
    */
   public void clean() throws SQLException
   {
      recursiveClean(Constants.ROOT_PARENT_UUID);
   }

   private void recursiveClean(String parentID) throws SQLException
   {
      PreparedStatement selectStatement = null;
      PreparedStatement removeStatement = null;
      ResultSet result = null;

      try
      {
         selectStatement = connection.prepareStatement(this.selectItems);
         selectStatement.setString(1, parentID);

         final PreparedStatement fSelectStatement = selectStatement;
         result = (ResultSet)SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
         {
            public Object run() throws Exception
            {
               return fSelectStatement.executeQuery();
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
            removeStatement = connection.prepareStatement(this.removeItems);
            removeStatement.setString(1, parentID);

            final PreparedStatement fRemoveStatement = removeStatement;
            SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  fRemoveStatement.executeUpdate();
                  return null;
               }
            });
         }
      }
      finally
      {
         if (selectStatement != null)
         {
            selectStatement.close();
         }

         if (removeStatement != null)
         {
            removeStatement.close();
         }

         if (result != null)
         {
            result.close();
         }
      }
   }
}
