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
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JCR Storage DB2 initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */

public class H2DBInitializer extends DBInitializer
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DB2DBInitializer");

   public H2DBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   private int getSequenceStartValue(final Connection conn) throws SQLException
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
    * {@inheritDoc}
    */
   @Override
   protected String updateQuery(String sql)
   {
      try
      {
         if ((creatSequencePattern.matcher(sql)).find())
         {
            sql = sql.concat(" Start with " + Integer.toString(getSequenceStartValue(connection)+1));
         }
      }
      catch (SQLException e)
      {
         LOG.debug("SQLException occurs while update the sequence start value", e);
      }
      return sql;
   }
}