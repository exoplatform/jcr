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

import org.exoplatform.services.jcr.impl.InspectionLog.InspectionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: InspectionQueryFilteredMultivaluedProperties.java 34360 16.02.2012 andrew.plotnikov $
 */
class InspectionQueryFilteredMultivaluedProperties extends InspectionQuery
{

   /**
    * {@inheritDoc}
    */
   public InspectionQueryFilteredMultivaluedProperties(String statement, String[] fieldNames, String headerMessage, InspectionStatus status)
   {
      super(statement, fieldNames, headerMessage, status);
   }

   /**
    * {@inheritDoc}
    */
   public PreparedStatement prepareStatement(Connection connection) throws SQLException
   {
      PreparedStatement preparedStatement = super.prepareStatement(connection);
      preparedStatement.setBoolean(1, false);

      return preparedStatement;
   }

}
