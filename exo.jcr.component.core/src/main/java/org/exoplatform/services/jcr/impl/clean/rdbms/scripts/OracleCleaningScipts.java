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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: OracleDBCleanScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 *
 */
public class OracleCleaningScipts extends DBCleaningScripts
{

   /**
    * OracleCleanScipts constructor.
    */
   public OracleCleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareRenamingApproachScripts();
   }

   /**
    * OracleCleanScipts constructor.
    */
   public OracleCleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
   {
      super(dialect, wEntry);

      if (multiDb)
      {
         prepareRenamingApproachScripts();
      }
      else
      {
         prepareSimpleCleaningApproachScripts();
      }
   }

   /**
    * R{@inheritDoc}
    */
   protected Collection<String> getConstraintsAddingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_PK_" + valueTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + itemTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + valueTableSuffix + "_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES " + itemTableName + "(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + refTableSuffix + " PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)";
      scripts.add("ALTER TABLE " + refTableName + " ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getConstraintsRemovingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_PK_" + valueTableSuffix;
      scripts.add("ALTER TABLE " + valueTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_FK_" + valueTableSuffix + "_PROPERTY";
      scripts.add("ALTER TABLE " + valueTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + itemTableSuffix;
      scripts.add("ALTER TABLE " + itemTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + refTableSuffix;
      scripts.add("ALTER TABLE " + refTableName + " DROP CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_FK");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_NAME");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_ID");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_NAME");
      scripts.add("DROP INDEX JCR_IDX_" + valueTableSuffix + "_PROPERTY");
      scripts.add("DROP INDEX JCR_IDX_" + refTableSuffix + "_PROPERTY");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesAddingScripts() throws DBCleanException
   {
      Collection<String> scripts = new ArrayList<String>();

      try
      {
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT_FK ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT ON " + itemTableName,
            multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT_NAME ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT_ID ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_NAME ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + valueTableSuffix + "_PROPERTY ON "
            + valueTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + refTableSuffix + "_PROPERTY ON " + refTableName,
            multiDb, dialect, wsEntry));
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
      catch (IOException e)
      {
         throw new DBCleanException(e);
      }

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getTablesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP SEQUENCE " + valueTableName + "_SEQ");
      scripts.addAll(super.getTablesDroppingScripts());

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP SEQUENCE " + valueTableName + "_SEQ_OLD");
      scripts.addAll(super.getOldTablesDroppingScripts());

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      // JCR_VALUE
      scripts.add("ALTER TABLE " + valueTableName + " RENAME TO " + valueTableName + "_OLD");
      scripts.add("ALTER TABLE " + valueTableName + "_OLD" + " RENAME CONSTRAINT JCR_PK_" + valueTableSuffix
         + " TO JCR_PK_" + valueTableSuffix + "_OLD");
      scripts.add("ALTER TABLE " + valueTableName + "_OLD" + " RENAME CONSTRAINT JCR_FK_" + valueTableSuffix
         + "_PROPERTY TO JCR_FK_" + valueTableSuffix + "_PROPERTY_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + valueTableSuffix + " RENAME TO JCR_PK_" + valueTableSuffix + "_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + valueTableSuffix + "_PROPERTY RENAME TO JCR_IDX_" + valueTableSuffix
         + "_PROPERTY_OLD");

      // TRIGGER and SEQ
      scripts.add("RENAME " + valueTableName + "_SEQ TO " + valueTableName + "_SEQ_OLD");
      scripts.add("DROP TRIGGER BI_" + valueTableName);

      // JCR_ITEM
      scripts.add("ALTER TABLE " + itemTableName + " RENAME TO " + itemTableName + "_OLD");
      scripts.add("ALTER TABLE " + itemTableName + "_OLD RENAME CONSTRAINT JCR_PK_" + itemTableSuffix + " TO JCR_PK_"
         + itemTableSuffix + "_OLD");
      scripts.add("ALTER TABLE " + itemTableName + "_OLD RENAME CONSTRAINT JCR_FK_" + itemTableSuffix
         + "_PARENT TO JCR_FK_" + itemTableSuffix + "_PARENT_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + itemTableSuffix + " RENAME TO JCR_PK_" + itemTableSuffix + "_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_FK RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_FK_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_NAME RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_NAME_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_ID RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_ID_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM RENAME TO JCR_IDX_" + itemTableSuffix
         + "_N_ORDER_NUM_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_NAME RENAME TO JCR_IDX_" + itemTableSuffix
         + "_NAME_OLD");

      // JCR_REF
      scripts.add("ALTER TABLE " + refTableName + " RENAME TO " + refTableName + "_OLD");
      scripts.add("ALTER TABLE " + refTableName + "_OLD RENAME CONSTRAINT JCR_PK_" + refTableSuffix + " TO JCR_PK_"
         + refTableSuffix + "_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + refTableSuffix + " RENAME TO JCR_PK_" + refTableSuffix + "_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + refTableSuffix + "_PROPERTY RENAME TO JCR_IDX_" + refTableSuffix
         + "_PROPERTY_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts() throws DBCleanException
   {
      Collection<String> scripts = new ArrayList<String>();

      // VALUE
      scripts.add("ALTER TABLE " + valueTableName + "_OLD RENAME TO " + valueTableName + "");
      scripts.add("ALTER TABLE " + valueTableName + " RENAME CONSTRAINT JCR_PK_" + valueTableSuffix + "_OLD TO JCR_PK_"
         + valueTableSuffix);
      scripts.add("ALTER TABLE " + valueTableName + " RENAME CONSTRAINT JCR_FK_" + valueTableSuffix
         + "_PROPERTY_OLD TO JCR_FK_" + valueTableSuffix + "_PROPERTY");
      scripts.add("ALTER INDEX JCR_PK_" + valueTableSuffix + "_OLD RENAME TO JCR_PK_" + valueTableSuffix);
      scripts.add("ALTER INDEX JCR_IDX_" + valueTableSuffix + "_PROPERTY_OLD RENAME TO JCR_IDX_" + valueTableSuffix
         + "_PROPERTY");

      // TRIGGER and SEQ
      scripts.add("RENAME " + valueTableName + "_SEQ_OLD TO " + valueTableName + "_SEQ");
      try
      {
         scripts.add(DBInitializerHelper.getObjectScript("CREATE OR REPLACE trigger", multiDb, dialect, wsEntry));
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
      catch (IOException e)
      {
         throw new DBCleanException(e);
      }

      // ITEM
      scripts.add("ALTER TABLE " + itemTableName + "_OLD RENAME TO " + itemTableName + "");
      scripts.add("ALTER TABLE " + itemTableName + " RENAME CONSTRAINT JCR_PK_" + itemTableSuffix + "_OLD TO JCR_PK_"
         + itemTableSuffix);
      scripts.add("ALTER TABLE " + itemTableName + " RENAME CONSTRAINT JCR_FK_" + itemTableSuffix
         + "_PARENT_OLD TO JCR_FK_" + itemTableSuffix + "_PARENT");

      scripts.add("ALTER INDEX JCR_PK_" + itemTableSuffix + "_OLD RENAME TO JCR_PK_" + itemTableSuffix);
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_FK_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_FK");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_NAME_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_NAME");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_ID_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_ID");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_N_ORDER_NUM");
      scripts.add("ALTER INDEX JCR_IDX_" + itemTableSuffix + "_NAME_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_NAME");

      // REF
      scripts.add("ALTER TABLE " + refTableName + "_OLD RENAME TO " + refTableName + "");
      scripts.add("ALTER TABLE " + refTableName + " RENAME CONSTRAINT JCR_PK_" + refTableSuffix + "_OLD TO JCR_PK_"
         + refTableSuffix);
      scripts.add("ALTER INDEX JCR_PK_" + refTableSuffix + "_OLD RENAME TO JCR_PK_" + refTableSuffix);
      scripts.add("ALTER INDEX JCR_IDX_" + refTableSuffix + "_PROPERTY_OLD RENAME TO JCR_IDX_" + refTableSuffix
         + "_PROPERTY");

      return scripts;
   }

}
