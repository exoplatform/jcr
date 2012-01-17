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
 * @version $Id: MySQLCleaningScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 *
 */
public class MySQLCleaningScipts extends DBCleaningScripts
{

   /**
    * MySQLCleaningScipts constructor.
    */
   public MySQLCleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareRenamingApproachScripts();
   }

   /**
    * MySQLCleaningScipts constructor.
    */
   public MySQLCleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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

      // constraints already removed in {@link #getDBInitializationScripts()}
      cleaningScripts.clear();
      cleaningScripts.addAll(getTablesRenamingScripts());
      cleaningScripts.addAll(getDBInitializationScripts());
      cleaningScripts.addAll(getIndexesDroppingScripts());

      String constraintName =
         "JCR_FK_" + tablePrefix + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
      committingScripts.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD CONSTRAINT " + constraintName);
   }

   /**
    * {@inheritDoc}
    */
   protected String constraintDroppingSyntax()
   {
      return "DROP FOREIGN KEY";
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getDBInitializationScripts() throws DBCleanException
   {
      Collection<String> scripts = super.getDBInitializationScripts();

      return filter(scripts);
   }

   /**
    * Removing foreign key creation from initialization scripts for table JCR_S(M)ITEM 
    * and JCR_S(M)VALUE. It is not possible to create table with such foreign key if the same key 
    * exists in another table of database
    */
   private Collection<String> filter(Collection<String> scripts)
   {
      String JCR_ITEM_PRIMARY_KEY = "CONSTRAINT JCR_PK_" + tablePrefix + "ITEM PRIMARY KEY(ID)";
      String JCR_ITEM_FOREIGN_KEY =
         "CONSTRAINT JCR_FK_" + tablePrefix + "ITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + tablePrefix
            + "ITEM(ID)";

      String JCR_VALUE_PRIMARY_KEY = "CONSTRAINT JCR_PK_" + tablePrefix + "VALUE PRIMARY KEY(ID)";
      String JCR_VALUE_FOREIGN_KEY =
         "CONSTRAINT JCR_FK_" + tablePrefix + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + tablePrefix
            + "ITEM(ID)";

      Collection<String> filteredScripts = new ArrayList<String>();

      for (String script : scripts)
      {
         if (script.contains(JCR_ITEM_PRIMARY_KEY + ","))
         {
            script = script.replace(JCR_ITEM_PRIMARY_KEY + ",", JCR_ITEM_PRIMARY_KEY);
            script = script.replace(JCR_ITEM_FOREIGN_KEY, "");
         }
         else if (script.contains(JCR_VALUE_PRIMARY_KEY + ","))
         {
            script = script.replace(JCR_VALUE_PRIMARY_KEY + ",", JCR_VALUE_PRIMARY_KEY);
            script = script.replace(JCR_VALUE_FOREIGN_KEY, "");
         }

         filteredScripts.add(script);
      }

      return filteredScripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE RENAME TO JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM RENAME TO JCR_" + tablePrefix + "ITEM_OLD");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF RENAME TO JCR_" + tablePrefix + "REF_OLD");

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM_OLD RENAME TO JCR_" + tablePrefix + "ITEM");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "VALUE_OLD RENAME TO JCR_" + tablePrefix + "VALUE");
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "REF_OLD RENAME TO JCR_" + tablePrefix + "REF");

      return scripts;
   }

}
