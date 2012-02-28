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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: InspectionQuery.java 34360 16.02.2012 andrew.plotnikov $
 */
public class InspectionQuery
{
   /**
    * Data class, contains a combination of SQL states, description, field names and status  
    */

   /**
    * SQL query that must be executed.
    */
   public String statement;

   /**
    * Inspection query description.
    */
   public String description;

   /**
    * Field names that must be showed in inspection log if something wrong.
    */
   public String[] fieldNames;

   public InspectionQuery(String statement, String[] fieldNames, String headerMessage)
   {
      this.statement = statement;
      this.description = headerMessage;
      this.fieldNames = fieldNames;
   }

   public String getStatement()
   {
      return statement;
   }

   public String getDescription()
   {
      return description;
   }

   public String[] getFieldNames()
   {
      return fieldNames;
   }

   /**
    * Creates a PreparedStatement object for sending parameterized SQL statements to the database. 
    * 
    * @param connection
    *          connection to workspace storage
    * @return
    *          a new default PreparedStatement object containing the pre-compiled SQL statement 
    * @throws SQLException
    *           if a database access error occurs or this method is called on a closed connection
    */
   public PreparedStatement prepareStatement(Connection connection) throws SQLException
   {
      return connection.prepareStatement(statement);
   }

}
