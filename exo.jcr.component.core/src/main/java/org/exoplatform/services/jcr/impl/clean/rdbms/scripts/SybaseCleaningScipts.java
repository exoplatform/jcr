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

package org.exoplatform.services.jcr.impl.clean.rdbms.scripts;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
   protected Collection<String> getConstraintsAddingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_PK_" + itemTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + valueTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + valueTableSuffix + "_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES " + itemTableName + "(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getConstraintsRemovingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_FK_" + valueTableSuffix + "_PROPERTY";
      scripts.add("ALTER TABLE  " + valueTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + itemTableSuffix;
      scripts.add("ALTER TABLE  " + itemTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + valueTableSuffix;
      scripts.add("ALTER TABLE  " + valueTableName + " DROP CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP INDEX " + itemTableName + ".JCR_IDX_" + itemTableSuffix + "_PARENT");
      scripts.add("DROP INDEX " + itemTableName + ".JCR_IDX_" + itemTableSuffix + "_PARENT_NAME");
      scripts.add("DROP INDEX " + itemTableName + ".JCR_IDX_" + itemTableSuffix + "_PARENT_ID");
      scripts.add("DROP INDEX " + itemTableName + ".JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM");
      scripts.add("DROP INDEX " + itemTableName + ".JCR_IDX_" + itemTableSuffix + "_NAME");
      scripts.add("DROP INDEX " + valueTableName + ".JCR_IDX_" + valueTableSuffix + "_PROPERTY");
      scripts.add("DROP INDEX " + refTableName + ".JCR_IDX_" + refTableSuffix + "_PROPERTY");

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
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + itemTableSuffix + "_PARENT ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_NAME ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_ID ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + itemTableSuffix + "_NAME ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("INDEX JCR_IDX_" + valueTableSuffix + "_PROPERTY ON "
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
   protected Collection<String> getTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("sp_rename " + valueTableName + ", " + valueTableName + "_OLD");
      scripts.add("sp_rename " + itemTableName + ", " + itemTableName + "_OLD");
      scripts.add("sp_rename " + refTableName + ", " + refTableName + "_OLD");
      scripts.add("sp_rename JCR_FK_" + valueTableSuffix + "_PROPERTY, JCR_FK_" + valueTableSuffix + "_PROPERTY_OLD");
      scripts.add("sp_rename JCR_FK_" + itemTableSuffix + "_PARENT, JCR_FK_" + itemTableSuffix + "_PARENT_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()  throws DBCleanException
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("sp_rename " + valueTableName + "_OLD, " + valueTableName);
      scripts.add("sp_rename " + itemTableName + "_OLD, " + itemTableName);
      scripts.add("sp_rename " + refTableName + "_OLD, " + refTableName);

      scripts.add("sp_rename JCR_FK_" + valueTableSuffix + "_PROPERTY_OLD, JCR_FK_" + valueTableSuffix + "_PROPERTY");
      scripts.add("sp_rename JCR_FK_" + itemTableSuffix + "_PARENT_OLD, JCR_FK_" + itemTableSuffix + "_PARENT");

      return scripts;
   }

}
