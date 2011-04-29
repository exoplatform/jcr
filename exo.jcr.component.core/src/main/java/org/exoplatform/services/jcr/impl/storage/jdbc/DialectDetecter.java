/*
 * Copyright (C) 2010 eXo Platform SAS.
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

import org.exoplatform.commons.utils.SecurityHelper;

import java.security.PrivilegedExceptionAction;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * JDBC dialect detecter based on database metadata and vendor product name.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id:DialectDetecter.java 1111 2010-01-01 00:00:01Z pnedonosko $
 *
 */
public class DialectDetecter
{

   /**
    * Detect databse dialect using JDBC metadata. Based on code of 
    * http://svn.jboss.org/repos/hibernate/core/trunk/core/src/main/java/org/hibernate/
    * dialect/resolver/StandardDialectResolver.java 
    * 
    * @param jdbcConn Connection 
    * @return String
    * @throws SQLException if error occurs
    */
   public static String detect(final DatabaseMetaData metaData) throws SQLException
   {
      final String databaseName =
         SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<String>()
         {
            public String run() throws Exception
            {
               return metaData.getDatabaseProductName();
            }
         });

      if ("HSQL Database Engine".equals(databaseName))
      {
         return DBConstants.DB_DIALECT_HSQLDB;
      }

      if ("H2".equals(databaseName))
      {
         return DBConstants.DB_DIALECT_H2;
      }

      if ("MySQL".equals(databaseName))
      {
         // TODO doesn't detect MySQL_UTF8
         return DBConstants.DB_DIALECT_MYSQL;
      }

      if ("PostgreSQL".equals(databaseName))
      {
         return DBConstants.DB_DIALECT_PGSQL;
      }

      if ("Apache Derby".equals(databaseName))
      {
         return DBConstants.DB_DIALECT_DERBY;
      }

      if ("ingres".equalsIgnoreCase(databaseName))
      {
         return DBConstants.DB_DIALECT_INGRES;
      }

      if (databaseName.startsWith("Microsoft SQL Server"))
      {
         return DBConstants.DB_DIALECT_MSSQL;
      }

      if ("Sybase SQL Server".equals(databaseName) || "Adaptive Server Enterprise".equals(databaseName))
      {
         return DBConstants.DB_DIALECT_SYBASE;
      }

      if (databaseName.startsWith("Adaptive Server Anywhere"))
      {
         // TODO not implemented anything special for
         return DBConstants.DB_DIALECT_SYBASE;
      }

      // TODO Informix not supported now
      //if ( "Informix Dynamic Server".equals( databaseName ) ) {
      //   return new InformixDialect();
      //}

      if (databaseName.startsWith("DB2/"))
      {
         // TODO doesn't detect DB2 v8 
         return DBConstants.DB_DIALECT_DB2;
      }

      if ("Oracle".equals(databaseName))
      {
         // TODO doesn't detect Oracle OCI (experimental support still)
         return DBConstants.DB_DIALECT_ORACLE;

         //         int databaseMajorVersion = metaData.getDatabaseMajorVersion();
         //         switch ( databaseMajorVersion ) {
         //            case 11:
         //               log.warn( "Oracle 11g is not yet fully supported; using 10g dialect" );
         //               return new Oracle10gDialect();
         //            case 10:
         //               return new Oracle10gDialect();
         //            case 9:
         //               return new Oracle9iDialect();
         //            case 8:
         //               return new Oracle8iDialect();
         //            default:
         //               log.warn( "unknown Oracle major version [" + databaseMajorVersion + "]" );
         //         }
      }

      return DBConstants.DB_DIALECT_GENERIC;
   }
}
