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
import org.exoplatform.services.database.utils.JDBCUtils;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JCR Storage HSQL initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */

public class HSQLDBInitializer extends StorageDBInitializer
{

   public HSQLDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isSequenceExists(final Connection conn, final String sequenceName) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Boolean>()
      {
         public Boolean run()
         {
            return sequenceExists(sequenceName, conn);
         }
      });
   }

   private boolean sequenceExists(String sequenceName, Connection con)
   {
      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query;
         query = "SELECT count(*) FROM information_schema.system_sequences where  SEQUENCE_NAME='" + sequenceName + "'";

         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         return (trs.next() && trs.getInt(1) >= 1);

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the sequence " + sequenceName, e);
         }
         return false;
      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
   }
}