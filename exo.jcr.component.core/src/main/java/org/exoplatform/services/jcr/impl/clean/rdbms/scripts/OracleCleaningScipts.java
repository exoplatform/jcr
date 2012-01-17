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

      String constraintName = "JCR_PK_" + tablePrefix + "VALUE PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "ITEM PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + tablePrefix + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "REF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getConstraintsRemovingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_PK_" + tablePrefix + "VALUE";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_FK_" + tablePrefix + "VALUE_PROPERTY";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "ITEM";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "REF";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF DROP CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_FK");
      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT");
      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID");
      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM");
      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "VALUE_PROPERTY");
      scripts.add("DROP INDEX JCR_IDX_" + tablePrefix + "REF_PROPERTY");

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
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "ITEM_PARENT_FK ON JCR_"
            + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "ITEM_PARENT ON JCR_" + tablePrefix
            + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID ON JCR_"
            + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM ON JCR_"
            + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "VALUE_PROPERTY ON JCR_"
            + tablePrefix + "VALUE", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + tablePrefix + "REF_PROPERTY ON JCR_"
            + tablePrefix + "REF", multiDb, dialect));
      }
      catch (RepositoryConfigurationException e)
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

      scripts.add("DROP TRIGGER BI_JCR_" + tablePrefix + "VALUE");
      scripts.add("DROP SEQUENCE JCR_" + tablePrefix + "VALUE_SEQ");

      scripts.addAll(super.getTablesDroppingScripts());

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP TRIGGER BI_JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("DROP SEQUENCE JCR_" + tablePrefix + "VALUE_SEQ_OLD");

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
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE RENAME TO JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE_OLD" + " RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "VALUE TO JCR_PK_" + tablePrefix + "VALUE_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE_OLD" + " RENAME CONSTRAINT JCR_FK_" + tablePrefix
         + "VALUE_PROPERTY TO JCR_FK_" + tablePrefix + "VALUE_PROPERTY_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "VALUE RENAME TO JCR_PK_" + tablePrefix + "VALUE_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "VALUE_PROPERTY RENAME TO JCR_IDX_" + tablePrefix
         + "VALUE_PROPERTY_OLD");

      // TRIGGER and SEQ
      scripts.add("RENAME JCR_" + tablePrefix + "VALUE_SEQ TO JCR_" + tablePrefix + "VALUE_SEQ_OLD");
      scripts.add("ALTER TRIGGER BI_JCR_" + tablePrefix + "VALUE RENAME TO BI_JCR_" + tablePrefix + "VALUE_OLD");

      // JCR_ITEM
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM RENAME TO JCR_" + tablePrefix + "ITEM_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM_OLD RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "ITEM TO JCR_PK_" + tablePrefix + "ITEM_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM_OLD RENAME CONSTRAINT JCR_FK_" + tablePrefix
         + "ITEM_PARENT TO JCR_FK_" + tablePrefix + "ITEM_PARENT_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "ITEM RENAME TO JCR_PK_" + tablePrefix + "ITEM_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_FK RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_FK_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_ID_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_N_ORDER_NUM_OLD");

      // JCR_REF
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF RENAME TO JCR_" + tablePrefix + "REF_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF_OLD RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "REF TO JCR_PK_" + tablePrefix + "REF_OLD");

      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "REF RENAME TO JCR_PK_" + tablePrefix + "REF_OLD");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "REF_PROPERTY RENAME TO JCR_IDX_" + tablePrefix
         + "REF_PROPERTY_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      // VALUE
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE_OLD RENAME TO JCR_" + tablePrefix + "VALUE");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "VALUE_OLD TO JCR_PK_" + tablePrefix + "VALUE");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE RENAME CONSTRAINT JCR_FK_" + tablePrefix
         + "VALUE_PROPERTY_OLD TO JCR_FK_" + tablePrefix + "VALUE_PROPERTY");
      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "VALUE_OLD RENAME TO JCR_PK_" + tablePrefix + "VALUE");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "VALUE_PROPERTY_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "VALUE_PROPERTY");

      // TRIGGER and SEQ
      scripts.add("RENAME JCR_" + tablePrefix + "VALUE_SEQ_OLD TO JCR_" + tablePrefix + "VALUE_SEQ");
      scripts.add("ALTER TRIGGER BI_JCR_" + tablePrefix + "VALUE_OLD RENAME TO BI_JCR_" + tablePrefix + "VALUE");

      // ITEM
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM_OLD RENAME TO JCR_" + tablePrefix + "ITEM");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "ITEM_OLD TO JCR_PK_" + tablePrefix + "ITEM");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM RENAME CONSTRAINT JCR_FK_" + tablePrefix
         + "ITEM_PARENT_OLD TO JCR_FK_" + tablePrefix + "ITEM_PARENT");
      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "ITEM_OLD RENAME TO JCR_PK_" + tablePrefix + "ITEM");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_FK_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_FK");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_ID");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_N_ORDER_NUM");

      // REF
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF_OLD RENAME TO JCR_" + tablePrefix + "REF");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF RENAME CONSTRAINT JCR_PK_" + tablePrefix
         + "REF_OLD TO JCR_PK_" + tablePrefix + "REF");
      scripts.add("ALTER INDEX JCR_PK_" + tablePrefix + "REF_OLD RENAME TO JCR_PK_" + tablePrefix + "REF");
      scripts.add("ALTER INDEX JCR_IDX_" + tablePrefix + "REF_PROPERTY_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "REF_PROPERTY");

      return scripts;
   }

}
