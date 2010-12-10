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

import org.exoplatform.services.jcr.config.WorkspaceEntry;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public class PgSQLSingleDBCleaner extends SingleDBCleaner
{
   /**
    * PgSQLSingleDBCleaner constructor.
    */
   public PgSQLSingleDBCleaner(WorkspaceEntry wsEntry, Connection connection)
   {
      super(wsEntry, connection);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      return super.isTableExists(conn, tableName.toLowerCase());
   }
}
