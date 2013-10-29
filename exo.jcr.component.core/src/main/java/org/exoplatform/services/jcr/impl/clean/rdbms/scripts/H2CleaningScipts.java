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


import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;

import java.util.ArrayList;
import java.util.Collection;


/**
 * @author <a href="aboughzela@exoplatform.com">Aymen Boughzela</a>
 * @version $Id: H2CleaningScipts.java $
 */
public class H2CleaningScipts extends DBCleaningScripts
{
   /**
    * H2CleanScipts constructor.
    */
   public H2CleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareRenamingApproachScripts();
   }

   /**
    * H2CleanScipts constructor.
    */
   public H2CleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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
   protected Collection<String> getTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      // renaming tables
      scripts.add("ALTER TABLE " + valueTableName + " RENAME TO " + valueTableName + "_OLD");
      scripts.add("ALTER TABLE " + itemTableName + " RENAME TO " + itemTableName + "_OLD");
      scripts.add("ALTER TABLE " + refTableName + " RENAME TO " + refTableName + "_OLD");

      // droping constraints
      scripts.add("ALTER TABLE " + valueTableName + "_OLD DROP CONSTRAINT JCR_FK_" + valueTableSuffix + "_PROPERTY");
      scripts.add("ALTER TABLE " + itemTableName + "_OLD DROP CONSTRAINT JCR_FK_" + itemTableSuffix + "_PARENT");
      scripts.add("ALTER TABLE " + itemTableName + "_OLD DROP CONSTRAINT JCR_PK_" + itemTableSuffix);
      scripts.add("ALTER TABLE " + valueTableName + "_OLD DROP CONSTRAINT JCR_PK_" + valueTableSuffix);
      scripts.add("ALTER TABLE " + refTableName + "_OLD DROP CONSTRAINT JCR_PK_" + refTableSuffix);

      // renaming indexes
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT_NAME RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_NAME_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT_ID RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_ID_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM RENAME TO JCR_IDX_" + itemTableSuffix
         + "_N_ORDER_NUM_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + valueTableSuffix + "_PROPERTY RENAME TO JCR_IDX_" + valueTableSuffix
         + "_PROPERTY_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + refTableSuffix + "_PROPERTY RENAME TO JCR_IDX_" + refTableSuffix
         + "_PROPERTY_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      // renaming tables
      scripts.add("ALTER TABLE " + valueTableName + "_OLD RENAME TO " + valueTableName);
      scripts.add("ALTER TABLE " + itemTableName + "_OLD RENAME TO " + itemTableName);
      scripts.add("ALTER TABLE " + refTableName + "_OLD RENAME TO " + refTableName);

      // creating constraints
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT JCR_PK_" + itemTableSuffix + " PRIMARY KEY(ID)");
      scripts.add("ALTER TABLE  " + valueTableName + " ADD CONSTRAINT JCR_FK_" + valueTableSuffix
         + "_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES " + itemTableName + "(ID)");
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT JCR_FK_" + itemTableSuffix
         + "_PARENT FOREIGN KEY(PARENT_ID) REFERENCES " + itemTableName + "(ID)");
      scripts.add("ALTER TABLE  " + valueTableName + " ADD CONSTRAINT JCR_PK_" + valueTableSuffix + " PRIMARY KEY(ID)");
      scripts.add("ALTER TABLE  " + refTableName + " ADD CONSTRAINT JCR_PK_" + refTableSuffix
         + " PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)");

      // renaming indexes
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT_NAME_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_NAME");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_PARENT_ID_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_PARENT_ID");
      scripts.add("ALTER INDEX  JCR_IDX_" + itemTableSuffix + "_N_ORDER_NUM_OLD RENAME TO JCR_IDX_" + itemTableSuffix
         + "_N_ORDER_NUM");
      scripts.add("ALTER INDEX  JCR_IDX_" + valueTableSuffix + "_PROPERTY_OLD RENAME TO JCR_IDX_" + valueTableSuffix
         + "_PROPERTY");
      scripts.add("ALTER INDEX  JCR_IDX_" + refTableSuffix + "_PROPERTY_OLD RENAME TO JCR_IDX_" + refTableSuffix
         + "_PROPERTY");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getTablesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("DROP SEQUENCE " + itemTableName + "_SEQ");

      scripts.addAll(super.getTablesDroppingScripts());

      return scripts;
   }

}
