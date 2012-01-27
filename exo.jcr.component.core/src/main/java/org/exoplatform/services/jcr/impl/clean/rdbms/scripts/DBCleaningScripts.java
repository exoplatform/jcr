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

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DBCleanScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class DBCleaningScripts
{
   protected final String tablePrefix;

   protected final String dialect;

   protected final boolean multiDb;

   protected final String workspaceName;

   protected final List<String> cleaningScripts = new ArrayList<String>();

   protected final List<String> committingScripts = new ArrayList<String>();

   protected final List<String> rollbackingScripts = new ArrayList<String>();

   /**
    * DBCleaningScripts constructor.
    * 
    * @throws DBCleanException 
    */
   DBCleaningScripts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      if (getMultiDbParameter(rEntry.getWorkspaceEntries().get(0)))
      {
         throw new DBCleanException("Not supported operation.");
      }

      this.multiDb = false;
      this.tablePrefix = "S";
      this.workspaceName = null;
      this.dialect = dialect;
   }

   /**
    * DBCleaningScripts constructor.
    * 
    * @throws DBCleanException
    */
   DBCleaningScripts(String dialect, WorkspaceEntry wsEntry) throws DBCleanException
   {
      this.multiDb = getMultiDbParameter(wsEntry);
      this.tablePrefix = multiDb ? "M" : "S";
      this.workspaceName = wsEntry.getName();
      this.dialect = dialect;
   }
   
   /**
    * Returns {@link #cleaningScripts}.
    */
   public Collection<String> getCleaningScripts()
   {
      return cleaningScripts;
   }

   /**
    * Returns {@link #committingScripts}.
    */
   public Collection<String> getCommittingScripts()
   {
      return committingScripts;
   }

   /**
    * Returns {@link #rollbackingScripts}.
    */
   public Collection<String> getRollbackingScripts()
   {
      return rollbackingScripts;
   }

   /**
    * Prepares scripts for renaming approach database cleaning.
    * 
    * @throws DBCleanException
    */
   protected void prepareRenamingApproachScripts() throws DBCleanException
   {
      cleaningScripts.addAll(getTablesRenamingScripts());
      cleaningScripts.addAll(getDBInitializationScripts());
      cleaningScripts.addAll(getFKRemovingScripts());
      cleaningScripts.addAll(getConstraintsRemovingScripts());
      cleaningScripts.addAll(getIndexesDroppingScripts());

      committingScripts.addAll(getOldTablesDroppingScripts());
      committingScripts.addAll(getIndexesAddingScripts());
      committingScripts.addAll(getConstraintsAddingScripts());
      committingScripts.addAll(getFKAddingScripts());

      rollbackingScripts.addAll(getTablesDroppingScripts());
      rollbackingScripts.addAll(getOldTablesRenamingScripts());
   }

   /**
    * Prepares scripts for dropping tables approach database cleaning.
    * 
    * @throws DBCleanException
    */
   protected void prepareDroppingTablesApproachScripts() throws DBCleanException
   {
      cleaningScripts.addAll(getTablesDroppingScripts());
      cleaningScripts.addAll(getDBInitializationScripts());
      cleaningScripts.addAll(getFKRemovingScripts());
      cleaningScripts.addAll(getIndexesDroppingScripts());

      committingScripts.addAll(getIndexesAddingScripts());
      committingScripts.addAll(getFKAddingScripts());
   }

   /**
    * Prepares scripts for simple cleaning database.
    */
   protected void prepareSimpleCleaningApproachScripts()
   {
      cleaningScripts.addAll(getFKRemovingScripts());
      cleaningScripts.addAll(getSingleDbWorkspaceCleaningScripts());

      committingScripts.addAll(getFKAddingScripts());

      rollbackingScripts.addAll(getFKAddingScripts());
   }

   /**
    * Returns SQL scripts for adding constraints.
    */
   protected Collection<String> getConstraintsAddingScripts()
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for removing constraints.
    */
   protected Collection<String> getConstraintsRemovingScripts()
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for renaming new JCR tables to new ones.
    */
   protected Collection<String> getOldTablesRenamingScripts()
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for renaming JCR tables to new ones.
    */
   protected Collection<String> getTablesRenamingScripts()
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for removing indexes.
    */
   protected Collection<String> getIndexesDroppingScripts()
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for adding indexes.
    * 
    * @throws DBCleanException 
    */
   protected Collection<String> getIndexesAddingScripts() throws DBCleanException
   {
      return new ArrayList<String>();
   }

   /**
    * Returns SQL scripts for removing FK on JCR_ITEM table.
    */
   protected Collection<String> getFKRemovingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName = "JCR_FK_" + tablePrefix + "ITEM_PARENT";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM " + constraintDroppingSyntax() + " " + constraintName);

      return scripts;
   }

   /**
    * Returns SQL scripts for adding FK on JCR_ITEM table.
    */
   protected Collection<String> getFKAddingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName =
         "JCR_FK_" + tablePrefix + "ITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + tablePrefix + "ITEM(ID)";
      scripts.add("ALTER TABLE JCR_" + tablePrefix + "ITEM ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * Returns SQL scripts for dropping existed old JCR tables.
    */
   protected Collection<String> getOldTablesDroppingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      scripts.add("DROP TABLE JCR_" + tablePrefix + "VALUE_OLD");
      scripts.add("DROP TABLE JCR_" + tablePrefix + "ITEM_OLD");
      scripts.add("DROP TABLE JCR_" + tablePrefix + "REF_OLD");

      return scripts;
   }

   /**
    * Returns SQL scripts for dropping existed JCR tables.
    */
   protected Collection<String> getTablesDroppingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      scripts.add("DROP TABLE JCR_" + tablePrefix + "VALUE");
      scripts.add("DROP TABLE JCR_" + tablePrefix + "ITEM");
      scripts.add("DROP TABLE JCR_" + tablePrefix + "REF");

      return scripts;
   }

   /**
    * 
    * @return
    */
   protected Collection<String> getSingleDbWorkspaceCleaningScripts()
   {
      List<String> scripts = new ArrayList<String>();

      scripts.add("delete from JCR_SVALUE where PROPERTY_ID IN (select ID from JCR_SITEM where CONTAINER_NAME='"
         + workspaceName + "')");
      scripts.add("delete from JCR_SREF where PROPERTY_ID IN (select ID from JCR_SITEM where CONTAINER_NAME='"
         + workspaceName + "')");
      scripts.add("delete from JCR_SITEM where CONTAINER_NAME='" + workspaceName + "'");

      return scripts;
   }

   /**
    * Returns SQL scripts for database initalization.
    * @throws DBCleanException 
    */
   protected Collection<String> getDBInitializationScripts() throws DBCleanException
   {
      String scriptPath = DBInitializerHelper.scriptPath(dialect, multiDb);

      String script;
      try
      {
         script = IOUtil.getStreamContentAsString(PrivilegedFileHelper.getResourceAsStream(scriptPath));
      }
      catch (FileNotFoundException e)
      {
         throw new DBCleanException(e);
      }
      catch (IllegalArgumentException e)
      {
         throw new DBCleanException(e);
      }
      catch (IOException e)
      {
         throw new DBCleanException(e);
      }

      List<String> scripts = new ArrayList<String>();
      for (String query : JDBCUtils.splitWithSQLDelimiter(script))
      {
         scripts.add(JDBCUtils.cleanWhitespaces(query));
      }

      scripts.add(DBInitializerHelper.getRootNodeInitializeScript(multiDb));

      return scripts;
   }

   /**
    * Returns the syntax for dropping constraint on database.  
    */
   protected String constraintDroppingSyntax()
   {
      return "DROP CONSTRAINT";
   }

   /**
    * Return {@link JDBCWorkspaceDataContainer#MULTIDB} parameter from workspace configuration.
    */
   private boolean getMultiDbParameter(WorkspaceEntry wsEntry) throws DBCleanException
   {
      try
      {
         return Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
   }
}
