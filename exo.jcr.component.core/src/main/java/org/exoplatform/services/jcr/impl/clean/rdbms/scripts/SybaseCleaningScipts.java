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
 * @version $Id: SybaseCleaningScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 *
 */
public class SybaseCleaningScipts extends DBCleaningScripts
{

   /**
    * SybaseCleaningScipts constructor.
    */
   public SybaseCleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareRenamingApproachScripts();
   }

   /**
    * SybaseCleaningScipts constructor.
    */
   public SybaseCleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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
    * {@inheritDoc}
    */
   protected void prepareRenamingApproachScripts() throws DBCleanException
   {
      super.prepareRenamingApproachScripts();

      String constraintName = "JCR_FK_" + tablePrefix + "VALUE_PROPERTY";
      cleaningScripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "ITEM";
      cleaningScripts.add("ALTER TABLE  JCR_" + tablePrefix + "ITEM DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "VALUE";
      cleaningScripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "ITEM PRIMARY KEY(ID)";
      committingScripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + tablePrefix + "VALUE PRIMARY KEY(ID)";
      committingScripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + tablePrefix + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)";
      committingScripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE ADD CONSTRAINT " + constraintName);
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP INDEX JCR_" + tablePrefix + "ITEM.JCR_IDX_" + tablePrefix + "ITEM_PARENT");
      scripts.add("DROP INDEX JCR_" + tablePrefix + "ITEM.JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID");
      scripts.add("DROP INDEX JCR_" + tablePrefix + "ITEM.JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM");
      scripts.add("DROP INDEX JCR_" + tablePrefix + "VALUE.JCR_IDX_" + tablePrefix + "VALUE_PROPERTY");
      scripts.add("DROP INDEX JCR_" + tablePrefix + "REF.JCR_IDX_" + tablePrefix + "REF_PROPERTY");

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
         scripts.add(DBInitializerHelper.getObjectScript("CREATE UNIQUE INDEX JCR_IDX_" + tablePrefix
            + "ITEM_PARENT ON JCR_" + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("CREATE UNIQUE INDEX JCR_IDX_" + multiDb
            + "ITEM_PARENT_ID ON JCR_" + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("CREATE UNIQUE INDEX JCR_IDX_" + multiDb
            + "ITEM_N_ORDER_NUM ON JCR_" + tablePrefix + "ITEM", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("CREATE UNIQUE INDEX JCR_IDX_" + multiDb
            + "VALUE_PROPERTY ON JCR_" + tablePrefix + "VALUE", multiDb, dialect));
         scripts.add(DBInitializerHelper.getObjectScript("CREATE UNIQUE INDEX JCR_IDX_" + multiDb
            + "REF_PROPERTY ON JCR_" + tablePrefix + "REF", multiDb, dialect));
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
   protected Collection<String> getTablesRenamingScripts()
   {
      Collection<String> scripts = super.getTableDroppingScripts();

      scripts.add("sp_rename JCR_" + tablePrefix + "VALUE, JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("sp_rename JCR_" + tablePrefix + "ITEM, JCR_" + tablePrefix + "ITEM_OLD");
      scripts.add("sp_rename JCR_" + tablePrefix + "REF, JCR_" + tablePrefix + "REF_OLD");

      scripts.add("sp_rename JCR_FK_" + tablePrefix + "VALUE_PROPERTY, JCR_FK_" + tablePrefix + "VALUE_PROPERTY_OLD");
      scripts.add("sp_rename JCR_FK_" + tablePrefix + "ITEM_PARENT, JCR_FK_" + tablePrefix + "ITEM_PARENT_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      Collection<String> scripts = super.getTableDroppingScripts();

      scripts.add("sp_rename JCR_" + tablePrefix + "VALUE_OLD, JCR_" + tablePrefix + "VALUE");
      scripts.add("sp_rename JCR_" + tablePrefix + "ITEM_OLD, JCR_" + tablePrefix + "ITEM");
      scripts.add("sp_rename JCR_" + tablePrefix + "REF_OLD, JCR_" + tablePrefix + "REF");

      scripts.add("sp_rename JCR_FK_" + tablePrefix + "VALUE_PROPERTY_OLD, JCR_FK_" + tablePrefix + "VALUE_PROPERTY");
      scripts.add("sp_rename JCR_FK_" + tablePrefix + "ITEM_PARENT_OLD, JCR_FK_" + tablePrefix + "ITEM_PARENT");

      return scripts;
   }

}
