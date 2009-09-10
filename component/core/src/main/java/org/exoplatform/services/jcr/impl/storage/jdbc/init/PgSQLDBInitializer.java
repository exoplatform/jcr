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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.03.2007
 * 
 * PgSQL convert all db object names to lower case, so respect it.
 * Same as Ingres initializer.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: PgSQLDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class PgSQLDBInitializer extends DBInitializer
{

   public PgSQLDBInitializer(String containerName, Connection connection, String scriptPath, boolean multiDb)
      throws IOException
   {
      super(containerName, connection, scriptPath, multiDb);
   }

   @Override
   protected boolean isIndexExists(Connection conn, String tableName, String indexName) throws SQLException
   {
      return super.isIndexExists(conn, tableName.toUpperCase().toLowerCase(), indexName.toUpperCase().toLowerCase());
   }

   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      return super.isTableExists(conn, tableName.toUpperCase().toLowerCase());
   }

}
