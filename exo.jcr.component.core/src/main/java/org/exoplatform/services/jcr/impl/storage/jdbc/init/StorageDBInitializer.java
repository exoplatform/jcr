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
package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JCR Storage DB initializer.
 * 
 * Created by The eXo Platform SAS 12.03.2007. 
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class StorageDBInitializer extends DBInitializer
{

   protected final boolean multiDb;

   public StorageDBInitializer(String containerName, Connection connection, String scriptPath, boolean multiDb)
      throws IOException
   {
      super(containerName, connection, scriptPath);
      this.multiDb = multiDb;
   }

   /**
    * Init root node parent record.
    */
   @Override
   protected void postInit(Connection connection) throws SQLException
   {
      final String MDB = (multiDb ? "M" : "S");
      String select =
         "select * from JCR_" + MDB + "ITEM where ID='" + Constants.ROOT_PARENT_UUID + "' and PARENT_ID='"
            + Constants.ROOT_PARENT_UUID + "'";

      if (!connection.createStatement().executeQuery(select).next())
      {
         String insert = DBInitializerHelper.getRootNodeInitializeScript(multiDb);

         connection.createStatement().executeUpdate(insert);
      }
   }

   /**
    * Init Start value for sequence.
    */
   protected int getStartValue(Connection con)
   {

      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         final String MDB = (multiDb ? "M" : "S");
         String query;

         if (JDBCUtils.tableExists("JCR_"+MDB+"ITEM", con))
         {
            query = "select max(N_ORDER_NUM) from JCR_"+MDB+"ITEM";
         }
         else
         {
            return -1;
         }
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) > 0)
         {
            return trs.getInt(1);
         }
         else
         {
            return -1;
         }

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while update the sequence start value", e);
         }
         return -1;
      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
   }

}
