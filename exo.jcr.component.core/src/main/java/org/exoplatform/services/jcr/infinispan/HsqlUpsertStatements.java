/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The upsert of a JDBC store on HSQLDB. Infinispan writes the entries of a JDBC store through a generic MERGE statement
 * on a database it has no dialect for, HSQLDB included: HSQLDB cannot type the parameters of its source table, and the
 * statement matches the rows on the data column instead of the id. A connection of this class prepares instead a MERGE
 * on the id, whose parameters are cast to the types of their columns. Infinispan binds the data, the timestamp, the id
 * and, in a segmented table, the segment, in that order.
 */
final class HsqlUpsertStatements implements InvocationHandler
{
   private static final Pattern GENERIC_MERGE =
      Pattern.compile("MERGE INTO (\\S+) USING \\(VALUES \\(\\?(?:, \\?)*\\)\\) AS tmp \\(([^)]+)\\) ON .*");

   private static final int ID_COLUMN = 2;

   private final Connection connection;

   private final Map<String, String> statements;

   private HsqlUpsertStatements(Connection connection, Map<String, String> statements)
   {
      this.connection = connection;
      this.statements = statements;
   }

   /**
    * @param connection a connection to an HSQLDB database
    * @param statements the statements already rewritten, by the statement Infinispan prepares
    * @return the connection, whose generic upsert of a JDBC store is replaced
    */
   static Connection wrap(Connection connection, Map<String, String> statements)
   {
      return (Connection)Proxy.newProxyInstance(HsqlUpsertStatements.class.getClassLoader(),
         new Class<?>[]{Connection.class}, new HsqlUpsertStatements(connection, statements));
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (method.getName().equals("prepareStatement") && args[0] instanceof String sql)
      {
         args[0] = rewrite(sql);
      }
      try
      {
         return method.invoke(connection, args);
      }
      catch (InvocationTargetException e)
      {
         throw e.getCause();
      }
   }

   private String rewrite(String sql) throws SQLException
   {
      String statement = statements.get(sql);
      if (statement == null)
      {
         Matcher merge = GENERIC_MERGE.matcher(sql);
         statement = merge.matches() ? typedMerge(merge.group(1), merge.group(2).split(", ")) : sql;
         statements.put(sql, statement);
      }
      return statement;
   }

   private String typedMerge(String table, String[] columns) throws SQLException
   {
      String id = columns[ID_COLUMN];
      StringBuilder values = new StringBuilder();
      StringBuilder update = new StringBuilder();
      StringBuilder insert = new StringBuilder();
      try (Statement statement = connection.createStatement();
           ResultSet empty = statement.executeQuery("SELECT " + String.join(", ", columns) + " FROM " + table
              + " WHERE 1 = 0"))
      {
         ResultSetMetaData metaData = empty.getMetaData();
         for (int i = 0; i < columns.length; i++)
         {
            String separator = i == 0 ? "" : ", ";
            values.append(separator).append("CAST(? AS ").append(type(metaData, i + 1)).append(')');
            insert.append(separator).append("tmp.").append(columns[i]);
            if (i != ID_COLUMN)
            {
               update.append(update.length() == 0 ? "" : ", ").append(columns[i]).append(" = tmp.").append(columns[i]);
            }
         }
      }
      return "MERGE INTO " + table + " USING (VALUES (" + values + ")) AS tmp (" + String.join(", ", columns) + ") ON ("
         + table + "." + id + " = tmp." + id + ") WHEN MATCHED THEN UPDATE SET " + update
         + " WHEN NOT MATCHED THEN INSERT (" + String.join(", ", columns) + ") VALUES (" + insert + ")";
   }

   private static String type(ResultSetMetaData metaData, int column) throws SQLException
   {
      String name = metaData.getColumnTypeName(column);
      switch (metaData.getColumnType(column))
      {
         case Types.CHAR :
         case Types.VARCHAR :
         case Types.NCHAR :
         case Types.NVARCHAR :
         case Types.BINARY :
         case Types.VARBINARY :
            return name + "(" + metaData.getPrecision(column) + ")";
         case Types.DECIMAL :
         case Types.NUMERIC :
            return name + "(" + metaData.getPrecision(column) + ", " + metaData.getScale(column) + ")";
         default :
            return name;
      }
   }
}
