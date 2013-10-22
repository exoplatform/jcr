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
import java.util.List;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DB2DBCleanScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DB2CleaningScipts extends DBCleaningScripts
{

   /**
    * DB2CleaningScipts constructor.
    */
   public DB2CleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareDroppingTablesApproachScripts();
   }

   /**
    * DB2CleaningScipts constructor.
    */
   public DB2CleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
   {
      super(dialect, wEntry);

      if (multiDb)
      {
         prepareDroppingTablesApproachScripts();
      }
      else
      {
         prepareSimpleCleaningApproachScripts();
      }
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getFKRemovingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_FK_" + valueTableSuffix + "_PROP";
      scripts.add("ALTER TABLE " + valueTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_FK_" + itemTableSuffix + "_PAREN";
      scripts.add("ALTER TABLE " + itemTableName + " DROP CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + valueTableSuffix;
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
   protected Collection<String> getFKAddingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_PK_" + valueTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + itemTableSuffix + " PRIMARY KEY(ID)";
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT " + constraintName);

      constraintName = "JCR_PK_" + refTableSuffix + " PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)";
      scripts.add("ALTER TABLE " + refTableName + " ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + valueTableSuffix + "_PROP FOREIGN KEY(PROPERTY_ID) REFERENCES " + itemTableName + "(ID)";
      scripts.add("ALTER TABLE " + valueTableName + " ADD CONSTRAINT " + constraintName);

      constraintName =
         "JCR_FK_" + itemTableSuffix + "_PAREN FOREIGN KEY(PARENT_ID) REFERENCES " + itemTableName + "(ID)";
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_NAME");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_PARENT_ID");
      scripts.add("DROP INDEX JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM");
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
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT ON " + itemTableName,
            multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT_NAME ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_PARENT_ID ON "
            + itemTableName, multiDb, dialect, wsEntry));
         scripts.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM ON "
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
   protected void prepareSimpleCleaningApproachScripts()
   {
      super.prepareSimpleCleaningApproachScripts();

      rollbackingScripts.clear();
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getSequencesDroppingScripts()
   {
      List<String> scripts = new ArrayList<String>();
      scripts.add("DROP SEQUENCE JCR_N"+itemTableSuffix);
      return scripts;
   }
}
