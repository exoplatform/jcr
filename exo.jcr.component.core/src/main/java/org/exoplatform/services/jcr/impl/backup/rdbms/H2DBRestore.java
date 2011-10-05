/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.backup.rdbms;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleaner;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: H2DBRestore.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class H2DBRestore extends DBRestore
{

   /**
    * Constructor H2DBRestore.
    */
   public H2DBRestore(File storageDir, Connection jdbcConn, Map<String, RestoreTableRule> tables,
      WorkspaceEntry wsConfig, FileCleaner fileCleaner, DBCleaner dbCleaner) throws NamingException, SQLException,
      RepositoryConfigurationException
   {
      super(storageDir, jdbcConn, tables, wsConfig, fileCleaner, dbCleaner);
   }

   /**
    * {@inheritDoc}
    */
   protected void prepareQueries(boolean isMultiDb)
   {
      super.prepareQueries(isMultiDb);

      // H2 doesn't contain primary key for JCR_SVALUE (JCR_MVALUE) table
      String constraintName = "JCR_PK_" + (isMultiDb ? "M" : "S") + "VALUE";
      addQueries.remove(constraintName);
      dropQueries.remove(constraintName);
   }
}
