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
package org.exoplatform.services.jcr.impl.clean.rdbms.scripts;

import org.exoplatform.services.database.utils.DialectConstants;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DBCleaningScriptsFactory.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DBCleaningScriptsFactory
{
   /**
    * Prepare SQL scripts for cleaning workspace data from database. 
    */
   public static DBCleaningScripts prepareScripts(String dialect, WorkspaceEntry wsEntry) throws DBCleanException
   {
      if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_MYISAM)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_MYISAM_UTF8)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_UTF8))
      {
         return new MySQLCleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_DB2)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_DB2V8))
      {
         return new DB2CleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MSSQL))
      {
         return new MSSQLCleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_PGSQL))
      {
         return new PgSQLCleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_SYBASE))
      {
         return new SybaseCleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_HSQLDB))
      {
         return new HSQLDBCleaningScipts(dialect, wsEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_ORACLE)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_ORACLEOCI))
      {
         return new OracleCleaningScipts(dialect, wsEntry);
      }
      else
      {
         throw new DBCleanException("Unsupported dialect " + dialect);
      }
   }

   /**
    * Prepare SQL scripts for cleaning repository data from database. 
    */
   public static DBCleaningScripts prepareScripts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_MYISAM)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_MYISAM_UTF8)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MYSQL_UTF8))
      {
         return new MySQLCleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_DB2)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_DB2V8))
      {
         return new DB2CleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_MSSQL))
      {
         return new MSSQLCleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_PGSQL))
      {
         return new PgSQLCleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_SYBASE))
      {
         return new SybaseCleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_HSQLDB))
      {
         return new HSQLDBCleaningScipts(dialect, rEntry);
      }
      else if (dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_ORACLE)
         || dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_ORACLEOCI))
      {
         return new OracleCleaningScipts(dialect, rEntry);
      }
      else
      {
         throw new DBCleanException("Unsupported dialect " + dialect);
      }
   }
}
