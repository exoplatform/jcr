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
package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JCR Storage DB initializer.
 * 
 * Created by The eXo Platform SAS 12.03.2007. 
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class StorageDBInitializer extends DBInitializer
{
   public StorageDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   protected String prepareScripts() throws IOException
   {
      if (containerConfig.dbStructureType == DatabaseStructureType.ISOLATED)
      {
         // Replace the names of Database entities 
         String scripts = super.prepareScripts();

         return scripts.replace("MITEM", "I" + containerConfig.dbTableSuffix)
            .replace("MVALUE", "V" + containerConfig.dbTableSuffix)
            .replace("MREF", "R" + containerConfig.dbTableSuffix);
      }
      else
      {
         return super.prepareScripts();
      }

   }

   /**
    * Init root node parent record.
    */
   @Override
   protected void postInit(Connection connection) throws SQLException
   {
      String tableSuffix = "";
      switch (containerConfig.dbStructureType)
      {
         case MULTI :
            tableSuffix = "MITEM";
            break;
         case SINGLE :
            tableSuffix = "SITEM";
            break;
         case ISOLATED :
            tableSuffix = "I" + containerConfig.dbTableSuffix;
            break;
      }
      String select =
         "select * from JCR_" + tableSuffix + " where ID='" + Constants.ROOT_PARENT_UUID + "' and PARENT_ID='"
            + Constants.ROOT_PARENT_UUID + "'";

      if (!connection.createStatement().executeQuery(select).next())
      {
         // TODO
         String insert = DBInitializerHelper.getRootNodeInitializeScript(containerConfig);

         connection.createStatement().executeUpdate(insert);
      }
   }
}
