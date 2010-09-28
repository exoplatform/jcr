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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: SingleDBCleaner.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SingleDBCleaner extends WorkspaceDBCleaner
{

   /**
    * Indicates if need to use clean helper.
    */
   protected final boolean postHelpClean;

   /**
    * DB clean helper.
    */
   protected final DBCleanHelper dbCleanHelper;

   /**
    * SingleDBCleaner constructor.
    */
   public SingleDBCleaner(String containerName, Connection connection)
   {
      this(containerName, connection, false);
   }

   /**
    * SingleDBCleaner constructor.
    */
   public SingleDBCleaner(String containerName, Connection connection, boolean postHelpClean)
   {
      super(containerName, connection);

      this.postHelpClean = postHelpClean;
      this.dbCleanHelper = new DBCleanHelper(containerName, connection);
      this.scripts =
         new String[]{
            "delete from JCR_SVALUE where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?)",
            "delete from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?)",
            "delete from JCR_SITEM where CONTAINER_NAME=?"};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clean() throws DBCleanerException
   {
      try
      {
         super.clean();

         if (postHelpClean)
         {
            dbCleanHelper.clean();
         }
      }
      finally
      {
         super.closeConnection();
      }
   }

   /**
    * {@inheritDoc}}
    */
   @Override
   protected void closeConnection()
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void executeQuery(Statement statement, String sql) throws SQLException
   {
      final String q = (containerName != null) ? sql.replace("?", "'" + containerName + "'") : sql;
      super.executeQuery(statement, q);
   }
}
