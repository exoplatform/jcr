/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.checker;


/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: InspectionQuery.java 34360 16.02.2012 andrew.plotnikov $
 */
public class InspectionQuery
{
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

   /**
    * Utility class which help to fix inconsistency.
    */
   public InconsistencyRepair repair;

   /**
    * Data class, contains a combination of SQL states, description, field names and status  
    */
   public InspectionQuery(String statement, String[] fieldNames, String headerMessage, InconsistencyRepair repair)
   {
      this.statement = statement;
      this.description = headerMessage;
      this.fieldNames = fieldNames;
      this.repair = repair;
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

   public InconsistencyRepair getRepair()
   {
      return repair;
   }
}
