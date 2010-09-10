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
package org.exoplatform.services.jcr.impl.util.jdbc;

import java.sql.Connection;

/**
 * The goal of this class is remove workspace data from database.
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleaner.java 111 2008-11-11 11:11:11Z serg $
 */
public class DBCleaner
{

   protected String REMOVE_ITEMS;

   protected String REMOVE_VALUES;

   protected String REMOVE_REFERENCES;

   protected String DROP_JCR_MITEM_TABLE;

   protected String DROP_JCR_MVALUE_TABLE;

   protected String DROP_MREF_TABLE;

   private final Connection connection;

   private final String containerName;

   /**
    * Constructor.
    * 
    * @param containerName - workspace name
    * @param connection - SQL conneciton
    */
   public DBCleaner(Connection connection, String containerName)
   {
      this.connection = connection;
      this.containerName = containerName;
      prepareQueries();
   }

   protected void prepareQueries()
   {

   }

   public void removeWorkspace()
   {

   }

   public void cleanupWorkspace()
   {
      //for single db support
      REMOVE_ITEMS = "delete from JCR_SITEM where CONTAINER_NAME=?";

      REMOVE_VALUES = "delete from JCR_SVALUE where PROPERTY_ID=?";
      REMOVE_REFERENCES = "delete from JCR_SREF where PROPERTY_ID=?";

      // for multi db support
      DROP_JCR_MITEM_TABLE = "DROP TABLE JCR_MITEM";
      DROP_JCR_MVALUE_TABLE = "DROP TABLE JCR_MVALUE";
      DROP_MREF_TABLE = "DROP TABLE JCR_MREF";

      //      DROP TABLE orders;
      //      DROP DATABASE mydatabase;
      //      DROP VIEW viewname;
      //      DROP INDEX orders.indexname;
      //
      //      -- FOR USE WITH ALTER COMMANDS
      //      DROP COLUMN column_name
      //      DROP FOREIGN KEY (foreign_key_name)
   }

}
