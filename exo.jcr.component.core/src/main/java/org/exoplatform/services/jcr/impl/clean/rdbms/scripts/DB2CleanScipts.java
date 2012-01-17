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
import java.util.List;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DB2DBCleanScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DB2CleanScipts extends DBCleaningScripts
{

   /**
    * DB2CleanScipts constructor.
    */
   public DB2CleanScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareDroppingTablesApproachScripts();
   }

   /**
    * DB2CleanScipts constructor.
    */
   public DB2CleanScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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
   protected Collection<String> getConstraintRemovingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_FK_" + tablePrefix + "ITEM_PAREN";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM DROP CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getConstraintAddingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName =
         "JCR_FK_" + tablePrefix + "ITEM_PAREN FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM ADD CONSTRAINT" + constraintName);

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
}
