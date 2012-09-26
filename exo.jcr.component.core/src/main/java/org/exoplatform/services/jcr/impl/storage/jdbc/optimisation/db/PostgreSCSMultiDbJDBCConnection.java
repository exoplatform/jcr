/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: PostgreSCSMultiDbJDBCConnection.java 34360 26 Sep 2012 andrew.plotnikov $
 *
 */
public class PostgreSCSMultiDbJDBCConnection extends PostgreMultiDbJDBCConnection
{

   protected static final String PATTERN_ESCAPE_STRING = "\\";

   /**
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public PostgreSCSMultiDbJDBCConnection(Connection dbConnection, boolean readOnly,
      JDBCDataContainerConfig containerConfig) throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   protected String getLikeExpressionEscape()
   {
      // must be .. LIKE 'prop\\_name' ESCAPE '\\'
      return this.PATTERN_ESCAPE_STRING;
   }

}
