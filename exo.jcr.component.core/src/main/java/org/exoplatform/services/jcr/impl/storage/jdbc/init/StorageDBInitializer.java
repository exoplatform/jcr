/*
 * Copyright (C) 2013 eXo Platform SAS.
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

import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;

/**
 * JCR Storage HSQL initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */

public class StorageDBInitializer extends DBInitializer
{
   public StorageDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String updateQuery(String sql)
   {
      try
      {
         Matcher tMatcher;
         if ((creatSequencePattern.matcher(sql)).find())
         {
            tMatcher = dbObjectNamePattern.matcher(sql);
            if (tMatcher.find())
            {
               if (sql.substring(tMatcher.start(), tMatcher.end()).equals("JCR_"+DBInitializerHelper.getItemTableSuffix(containerConfig)+"SEQ"))
               {
                  sql = sql.concat(" Start with " + Integer.toString(getSequenceStartValue(connection) ));
               }
            }
         }
      }
      catch (SQLException e)
      {
         LOG.debug("SQLException occurs while update the sequence start value", e);
      }
      return sql;
   }

   protected int getSequenceStartValue(final Connection conn) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Integer>()
      {
         public Integer run()
         {
            return getStartValue(conn);
         }
      });
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
         String query;

         String tableItem = DBInitializerHelper.getItemTableName(containerConfig);
         if (JDBCUtils.tableExists(tableItem, con))
         {
            query = "select max(N_ORDER_NUM) from " + tableItem;
         }
         else
         {
            return -1;
         }
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) >= 0)
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
