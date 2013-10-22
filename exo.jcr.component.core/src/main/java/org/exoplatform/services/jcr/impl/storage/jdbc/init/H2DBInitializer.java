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
               if (sql.substring(tMatcher.start(), tMatcher.end()).equals("JCR_N"+DBInitializerHelper.getItemTableSuffix(containerConfig)))
               {
                  sql = sql.concat(" Start with " + Integer.toString(getSequenceStartValue(connection)+1 ));
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