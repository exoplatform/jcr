/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.cas.JDBCValueContentAddressStorageImpl;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.util.Properties;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 19.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class TestCASableTreeFileIOChannel extends CASableFileIOChannelTestBase
{

   private ValueDataResourceHolder resources = new ValueDataResourceHolder();

   @Override
   protected void initVCAS() throws Exception
   {
      Properties props = new Properties();

      // find jdbc-source-name
      String jdbcSourceName = null;
      String jdbcDialect = null;
      for (WorkspaceEntry wse : repository.getConfiguration().getWorkspaceEntries())
      {
         if (wse.getName().equals(session.getWorkspace().getName()))
         {
            jdbcSourceName = wse.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
            jdbcDialect = DBInitializerHelper.getDatabaseDialect(wse);
         }
      }

      if (jdbcSourceName == null)
      {
         fail(JDBCWorkspaceDataContainer.SOURCE_NAME + " required in workspace container config");
      }

      props.put(JDBCValueContentAddressStorageImpl.JDBC_SOURCE_NAME_PARAM, jdbcSourceName);
      if (jdbcDialect != null)
      {
         props.put(JDBCValueContentAddressStorageImpl.JDBC_DIALECT_PARAM, jdbcDialect);
      }
      props.put(JDBCValueContentAddressStorageImpl.TABLE_NAME_PARAM,
         JDBCValueContentAddressStorageImpl.DEFAULT_TABLE_NAME + "_TEST");

      vcas = new JDBCValueContentAddressStorageImpl();
      vcas.init(props);
   }

   @Override
   protected FileIOChannel openCASChannel(String digestType) throws Exception
   {
      return new CASableTreeFileIOChannel(rootDir, fileCleaner, storageId, resources, vcas, digestType);
   }

}
