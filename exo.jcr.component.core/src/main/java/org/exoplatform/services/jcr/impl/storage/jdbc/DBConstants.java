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
package org.exoplatform.services.jcr.impl.storage.jdbc;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: DBConstants.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class DBConstants
{
   // ======================== Error constants ======================== 
   /**
    * JCR_PK_ITEM.
    */
   protected String JCR_PK_ITEM;

   /**
    * JCR_FK_ITEM_PARENT.
    */
   protected String JCR_FK_ITEM_PARENT;

   /**
    * JCR_IDX_ITEM_PARENT.
    */
   protected String JCR_IDX_ITEM_PARENT;

   /**
    * JCR_IDX_ITEM_PARENT_NAME.
    */
   protected String JCR_IDX_ITEM_PARENT_NAME;

   /**
    * JCR_IDX_ITEM_PARENT_ID.
    */
   protected String JCR_IDX_ITEM_PARENT_ID;

   /**
    * JCR_PK_VALUE.
    */
   protected String JCR_PK_VALUE;

   /**
    * JCR_FK_VALUE_PROPERTY.
    */
   protected String JCR_FK_VALUE_PROPERTY;

   /**
    * JCR_IDX_VALUE_PROPERTY.
    */
   protected String JCR_IDX_VALUE_PROPERTY;

   /**
    * JCR_PK_REF.
    */
   protected String JCR_PK_REF;

   /**
    * JCR_IDX_REF_PROPERTY.
    */
   protected String JCR_IDX_REF_PROPERTY;

   // ======================== SQL scripts ======================== 
   /**
    * FIND_ITEM_BY_ID.
    */
   protected String FIND_ITEM_BY_ID;

   /**
    * FIND_ITEM_BY_PATH.
    */
   protected String FIND_ITEM_BY_PATH;

   /**
    * FIND_ITEM_BY_NAME.
    */
   protected String FIND_ITEM_BY_NAME;

   /**
    * FIND_CHILD_PROPERTY_BY_PATH.
    */
   protected String FIND_CHILD_PROPERTY_BY_PATH;

   /**
    * FIND_PROPERTY_BY_NAME.
    */
   protected String FIND_PROPERTY_BY_NAME;

   /**
    * FIND_REFERENCES.
    */
   protected String FIND_REFERENCES;

   /**
    * FIND_VALUES_BY_PROPERTYID.
    */
   protected String FIND_VALUES_BY_PROPERTYID;

   /**
    * FIND_VALUE_BY_PROPERTYID_OREDERNUMB.
    */
   protected String FIND_VALUES_VSTORAGE_DESC_BY_PROPERTYID;

   /**
    * FIND_VALUE_BY_PROPERTYID_OREDERNUMB.
    */
   protected String FIND_VALUE_BY_PROPERTYID_OREDERNUMB;

   /**
    * FIND_NODES_BY_PARENTID.
    */
   protected String FIND_NODES_BY_PARENTID;

   /**
    * FIND_LAST_ORDER_NUMBER_BY_PARENTID.
    */
   protected String FIND_LAST_ORDER_NUMBER_BY_PARENTID;

   /**
    * FIND_NODES_COUNT_BY_PARENTID.
    */
   protected String FIND_NODES_COUNT_BY_PARENTID;

   /**
    * FIND_PROPERTIES_BY_PARENTID.
    */
   protected String FIND_PROPERTIES_BY_PARENTID;

   /**
    * FIND_MAX_PROPERTY_VERSIONS.
    */
   protected String FIND_MAX_PROPERTY_VERSIONS;

   /**
    * INSERT_NODE.
    */
   protected String INSERT_NODE;

   /**
    * INSERT_PROPERTY.
    */
   protected String INSERT_PROPERTY;

   /**
    * INSERT_VALUE.
    */
   protected String INSERT_VALUE;

   /**
    * INSERT_REF.
    */
   protected String INSERT_REF;

   /**
    * RENAME_NODE.
    */
   protected String RENAME_NODE;

   /**
    * UPDATE_NODE.
    */
   protected String UPDATE_NODE;

   /**
    * UPDATE_PROPERTY.
    */
   protected String UPDATE_PROPERTY;

   /**
    * DELETE_ITEM.
    */
   protected String DELETE_ITEM;

   /**
    * DELETE_VALUE.
    */
   protected String DELETE_VALUE;

   /**
    * DELETE_REF.
    */
   protected String DELETE_REF;

   /**
    * FIND_NODES.
    */
   protected String FIND_NODES_AND_PROPERTIES;

   // ======================== ITEMS table ======================== 
   /**
    * COLUMN_ID.
    */
   public static final String COLUMN_ID = "ID";

   /**
    * COLUMN_PARENTID.
    */
   public static final String COLUMN_PARENTID = "PARENT_ID";

   /**
    * COLUMN_NAME.
    */
   public static final String COLUMN_NAME = "NAME";

   /**
    * COLUMN_VERSION.
    */
   public static final String COLUMN_VERSION = "VERSION";

   /**
    * CONTAINER_NAME. Exists only for single-db.
    */
   public static final String CONTAINER_NAME = "CONTAINER_NAME";

   /**
    * COLUMN_CLASS.
    */
   public static final String COLUMN_CLASS = "I_CLASS";

   /**
    * COLUMN_INDEX.
    */
   public static final String COLUMN_INDEX = "I_INDEX";

   /**
    * COLUMN_NORDERNUM.
    */
   public static final String COLUMN_NORDERNUM = "N_ORDER_NUM";

   /**
    * COLUMN_PTYPE.
    */
   public static final String COLUMN_PTYPE = "P_TYPE";

   /**
    * COLUMN_PMULTIVALUED.
    */
   public static final String COLUMN_PMULTIVALUED = "P_MULTIVALUED";

   // VALUE table
   /**
    * PROPERTY_ID
    */
   public static final String COLUMN_VPROPERTY_ID = "PROPERTY_ID";

   /**
    * COLUMN_VDATA.
    */
   public static final String COLUMN_VDATA = "DATA";

   /**
    * COLUMN_VORDERNUM.
    */
   public static final String COLUMN_VORDERNUM = "ORDER_NUM";

   /**
    * COLUMN_VSTORAGE_DESC.
    */
   public static final String COLUMN_VSTORAGE_DESC = "STORAGE_DESC";

   // Dialects
   /**
    * DB_DIALECT_AUTO.
    */
   public final static String DB_DIALECT_AUTO = "Auto".intern();

   /**
    * DB_DIALECT_GENERIC.
    */
   public final static String DB_DIALECT_GENERIC = "Generic".intern();

   /**
    * DB_DIALECT_ORACLE.
    */
   public final static String DB_DIALECT_ORACLE = "Oracle".intern();

   /**
    * DB_DIALECT_ORACLEOCI.
    */
   public final static String DB_DIALECT_ORACLEOCI = "Oracle-OCI".intern();

   /**
    * DB_DIALECT_PGSQL.
    */
   public final static String DB_DIALECT_PGSQL = "PgSQL".intern();

   /**
    * DB_DIALECT_MYSQL.
    */
   public final static String DB_DIALECT_MYSQL = "MySQL".intern();

   /**
    * DB_DIALECT_MYSQL_UTF8.
    */
   public final static String DB_DIALECT_MYSQL_UTF8 = "MySQL-UTF8".intern();

   /**
    * DB_DIALECT_MYSQL_MYISAM.
    */
   public final static String DB_DIALECT_MYSQL_MYISAM = "MySQL-MyISAM".intern();

   /**
    * DB_DIALECT_MYSQL_MYISAM_UTF8.
    */
   public final static String DB_DIALECT_MYSQL_MYISAM_UTF8 = "MySQL-MyISAM-UTF8".intern();

   /**
    * DB_DIALECT_HSQLDB.
    */
   public final static String DB_DIALECT_HSQLDB = "HSQLDB".intern();

   /**
    * DB_DIALECT_DB2.
    */
   public final static String DB_DIALECT_DB2 = "DB2".intern();

   /**
    * DB_DIALECT_DB2V8.
    */
   public final static String DB_DIALECT_DB2V8 = "DB2V8".intern();

   /**
    * DB_DIALECT_MSSQL.
    */
   public final static String DB_DIALECT_MSSQL = "MSSQL".intern();

   /**
    * DB_DIALECT_SYBASE.
    */
   public final static String DB_DIALECT_SYBASE = "Sybase".intern();

   /**
    * DB_DIALECT_DERBY.
    */
   public final static String DB_DIALECT_DERBY = "Derby".intern();

   /**
    * DB_DIALECT_INGRES.
    */
   public final static String DB_DIALECT_INGRES = "Ingres".intern();

   /**
    * DB_DIALECT_H2.
    */
   public final static String DB_DIALECT_H2 = "H2".intern();

   /**
    * DB_DIALECTS.
    */
   public final static String[] DB_DIALECTS = {DB_DIALECT_GENERIC, DB_DIALECT_ORACLE, DB_DIALECT_ORACLEOCI,
      DB_DIALECT_PGSQL, DB_DIALECT_MYSQL, DB_DIALECT_HSQLDB, DB_DIALECT_DB2, DB_DIALECT_DB2V8, DB_DIALECT_MSSQL,
      DB_DIALECT_SYBASE, DB_DIALECT_DERBY, DB_DIALECT_MYSQL_UTF8, DB_DIALECT_INGRES, DB_DIALECT_H2,
      DB_DIALECT_MYSQL_MYISAM, DB_DIALECT_MYSQL_MYISAM_UTF8};

}
