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

import org.exoplatform.services.database.utils.JDBCUtils;
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
 * @version $Id: DBCleanScipts.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class DBCleaningScripts
{
   protected String itemTableName;

   protected String valueTableName;

   protected String refTableName;

   protected String itemTableSuffix;

   protected String valueTableSuffix;

   protected String refTableSuffix;

   protected  boolean useSequence;

   protected final String dialect;

   protected final String workspaceName;

   protected final boolean multiDb;

   protected final List<String> cleaningScripts = new ArrayList<String>();

   protected final List<String> committingScripts = new ArrayList<String>();

   protected final List<String> rollbackingScripts = new ArrayList<String>();

   protected final WorkspaceEntry wsEntry;

   /**
    * DBCleaningScripts constructor.
    * 
    * @throws DBCleanException 
    */
   DBCleaningScripts(String dialect, RepositoryEntry rEntry) throws DBCleanException
   {
      for (WorkspaceEntry wsEntry : rEntry.getWorkspaceEntries())
      {
         try
         {
            if (DBInitializerHelper.getDatabaseType(wsEntry).isMultiDatabase())
            {
               throw new DBCleanException("Not supported operation.");
            }
         }
         catch (RepositoryConfigurationException e)
         {
            throw new DBCleanException(e);
         }
      }

      this.multiDb = false;
      this.workspaceName = null;
      this.dialect = dialect;
      this.wsEntry = rEntry.getWorkspaceEntries().get(0);

      initTableNames(wsEntry);
   }

   /**
    * DBCleaningScripts constructor.
    * 
    * @throws DBCleanException
    */
   DBCleaningScripts(String dialect, WorkspaceEntry wsEntry) throws DBCleanException
   {
      try
      {
         this.multiDb = DBInitializerHelper.getDatabaseType(wsEntry).isMultiDatabase();
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }

      this.workspaceName = wsEntry.getName();
      this.dialect = dialect;
      this.wsEntry = wsEntry;

      initTableNames(wsEntry);
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
   protected Collection<String> getOldTablesRenamingScripts() throws DBCleanException
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

      String constraintName = "JCR_FK_" + itemTableSuffix + "_PARENT";
      scripts.add("ALTER TABLE " + itemTableName + " " + constraintDroppingSyntax() + " " + constraintName);

      return scripts;
   }

   /**
    * Returns SQL scripts for adding FK on JCR_ITEM table.
    */
   protected Collection<String> getFKAddingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      String constraintName =
         "JCR_FK_" + itemTableSuffix + "_PARENT FOREIGN KEY(PARENT_ID) REFERENCES " + itemTableName + "(ID)";
      scripts.add("ALTER TABLE " + itemTableName + " ADD CONSTRAINT " + constraintName);

      return scripts;
   }

   /**
    * Returns SQL scripts for dropping existed old JCR tables.
    */
   protected Collection<String> getOldTablesDroppingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      scripts.add("DROP TABLE " + valueTableName + "_OLD");
      scripts.add("DROP TABLE " + refTableName + "_OLD");
      scripts.add("DROP TABLE " + itemTableName + "_OLD");

      return scripts;
   }

   /**
    * Returns SQL scripts for dropping existed JCR tables.
    */
   protected Collection<String> getTablesDroppingScripts()
   {
      List<String> scripts = new ArrayList<String>();

      scripts.add("DROP TABLE " + valueTableName);
      scripts.add("DROP TABLE " + refTableName);
      scripts.add("DROP TABLE " + itemTableName);

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
      String dbScripts;
      try
      {
         dbScripts = DBInitializerHelper.prepareScripts(wsEntry, dialect);
      }
      catch (IOException e)
      {
         throw new DBCleanException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }

      List<String> scripts = new ArrayList<String>();
      for (String query : JDBCUtils.splitWithSQLDelimiter(dbScripts))
      {
         if (query.contains(itemTableName + "_SEQ") || query.contains(itemTableName + "_NEXT_VAL"))
         {
            continue;
         }
         scripts.add(JDBCUtils.cleanWhitespaces(query));
      }

      scripts.add(DBInitializerHelper.getRootNodeInitializeScript(itemTableName, multiDb));

      return scripts;
   }

   /**
    * Returns the syntax for dropping constraint on database.  
    */
   protected String constraintDroppingSyntax()
   {
      return "DROP CONSTRAINT";
   }

   private void initTableNames(WorkspaceEntry wsConfig) throws DBCleanException
   {
      try
      {
         itemTableName = DBInitializerHelper.getItemTableName(wsConfig);
         refTableName = DBInitializerHelper.getRefTableName(wsConfig);
         valueTableName = DBInitializerHelper.getValueTableName(wsConfig);

         itemTableSuffix = DBInitializerHelper.getItemTableSuffix(wsConfig);
         valueTableSuffix = DBInitializerHelper.getValueTableSuffix(wsConfig);
         refTableSuffix = DBInitializerHelper.getRefTableSuffix(wsConfig);
         useSequence= DBInitializerHelper.useSequenceForOrderNumber(wsConfig) ;
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
   }
}
