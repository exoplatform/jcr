/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;

/**
 * JCR Storage H2 initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */

public class H2DBInitializer extends StorageDBInitializer
{

   public H2DBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
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
               if (sql.substring(tMatcher.start(), tMatcher.end()).equals("JCR_" + DBInitializerHelper.getItemTableSuffix(containerConfig) + "SEQ"))
               {
                  sql = (containerConfig.useSequenceForOrderNumber) ? sql.concat(" Start with " + Integer.toString(getSequenceStartValue(connection) + 1)) : "";

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

}