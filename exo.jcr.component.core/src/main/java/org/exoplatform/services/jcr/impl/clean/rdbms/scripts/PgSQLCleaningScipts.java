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
 * @version $Id: PgSQLCleaningScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 *
 */
public class PgSQLCleaningScipts extends DBCleaningScripts
{

   /**
    * PgSQLCleaningScipts constructor.
    */
   public PgSQLCleaningScipts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      super(dialect, rEntry);

      prepareDroppingTablesApproachScripts();
   }

   /**
    * PgSQLCleaningScipts constructor.
    */
   public PgSQLCleaningScipts(String dialect, WorkspaceEntry wEntry) throws DBCleanException
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
   protected void prepareSimpleCleaningApproachScripts()
   {
      super.prepareSimpleCleaningApproachScripts();

      rollbackingScripts.clear();
   }

   /**
    * {@inheritDoc}
    */
   protected Collection<String> getTablesDroppingScripts()
   {
      Collection<String> scripts = new ArrayList<String>();

      if (useSequence)
      {
         scripts.add("DROP SEQUENCE " + itemTableName + "_SEQ");
      }

      scripts.addAll(super.getTablesDroppingScripts());

      return scripts;
   }
}
