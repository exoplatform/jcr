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
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: HSQLDBCleaningScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class HSQLDBCleaningScipts extends DBCleaningScripts
{
   /**
    * HSQLDBCleanScipts constructor.
    */
   public HSQLDBCleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareRenamingApproachScripts();
   }

   /**
    * HSQLDBCleanScipts constructor.
    */
   public HSQLDBCleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE RENAME TO JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM RENAME TO JCR_" + tablePrefix + "ITEM_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF RENAME TO JCR_" + tablePrefix + "REF_OLD");

      // droping constraints
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE_OLD DROP CONSTRAINT JCR_FK_" + tablePrefix
         + "VALUE_PROPERTY");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "ITEM_OLD DROP CONSTRAINT JCR_FK_" + tablePrefix + "ITEM_PARENT");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "ITEM_OLD DROP CONSTRAINT JCR_PK_" + tablePrefix + "ITEM");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE_OLD DROP CONSTRAINT JCR_PK_" + tablePrefix + "VALUE");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "REF_OLD DROP CONSTRAINT JCR_PK_" + tablePrefix + "REF");

      // renaming indexes
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_PARENT RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_ID_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_N_ORDER_NUM_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "VALUE_PROPERTY RENAME TO JCR_IDX_" + tablePrefix
         + "VALUE_PROPERTY_OLD");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "REF_PROPERTY RENAME TO JCR_IDX_" + tablePrefix
         + "REF_PROPERTY_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      // renaming tables
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE_OLD RENAME TO JCR_" + tablePrefix + "VALUE");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM_OLD RENAME TO JCR_" + tablePrefix + "ITEM");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF_OLD RENAME TO JCR_" + tablePrefix + "REF");

      // creating constraints
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "ITEM ADD CONSTRAINT JCR_PK_" + tablePrefix
         + "ITEM PRIMARY KEY(ID)");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE ADD CONSTRAINT JCR_FK_" + tablePrefix
         + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "ITEM ADD CONSTRAINT JCR_FK_" + tablePrefix
         + "ITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "VALUE ADD CONSTRAINT JCR_PK_" + tablePrefix
         + "VALUE PRIMARY KEY(ID)");
      scripts.add("ALTER TABLE  JCR_" + tablePrefix + "REF ADD CONSTRAINT JCR_PK_" + tablePrefix
         + "REF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)");

      // renaming indexes
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_PARENT_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_PARENT_ID_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_PARENT_ID");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "ITEM_N_ORDER_NUM_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "ITEM_N_ORDER_NUM");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "VALUE_PROPERTY_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "VALUE_PROPERTY");
      scripts.add("ALTER INDEX  JCR_IDX_" + tablePrefix + "REF_PROPERTY_OLD RENAME TO JCR_IDX_" + tablePrefix
         + "REF_PROPERTY");

      return scripts;
   }

}
