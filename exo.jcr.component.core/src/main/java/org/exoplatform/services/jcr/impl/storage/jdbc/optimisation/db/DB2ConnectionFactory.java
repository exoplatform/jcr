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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DB2ConnectionFactory.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DB2ConnectionFactory extends GenericCQConnectionFactory
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DB2ConnectionFactory");

   private Boolean isReindexingSupport;

   /**
    * DB2ConnectionFactory constructor.
    */
   public DB2ConnectionFactory(JDBCDataContainerConfig containerConfig) throws RepositoryException
   {
      super(containerConfig);
   }

   /**
    * DB2ConnectionFactory  constructor.
    */
   public DB2ConnectionFactory(DataSource dataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dataSource, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         if (this.containerConfig.dbStructureType.isSimpleTable())
         {
            return new DB2MultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
         }

         return new DB2SingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isIDNeededForPaging()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isReindexingSupport()
   {
      if (isReindexingSupport == null)
      {
         Connection con = null;
         try
         {
            con = getJdbcConnection();
            DatabaseMetaData metaData = con.getMetaData();
            if (log.isDebugEnabled())
            {
               log.debug("DB Major version = " + metaData.getDatabaseMajorVersion() + ", DB Minor version = "
                  + metaData.getDatabaseMinorVersion() + ", DB Product version = "
                  + metaData.getDatabaseProductVersion());
            }
            if (metaData.getDatabaseMajorVersion() > 9)
            {
               if (log.isDebugEnabled())
               {
                  log.debug("RDBMS indexing enabled as the major version is greater than 9.");
               }
               isReindexingSupport = true;
            }
            else if (metaData.getDatabaseMajorVersion() == 9 && metaData.getDatabaseMinorVersion() > 7)
            {
               if (log.isDebugEnabled())
               {
                  log.debug("RDBMS indexing enabled as the major version is 9 and the minor version is greater than 7.");
               }
               isReindexingSupport = true;
            }
            else if (metaData.getDatabaseMajorVersion() == 9 && metaData.getDatabaseMinorVersion() == 7)
            {
               // returned string like 'SQL09074'
               String value = metaData.getDatabaseProductVersion();
               int maintenanceVersion = Integer.parseInt(value.substring(value.length() - 1));
               isReindexingSupport = maintenanceVersion >= 2;
               if (log.isDebugEnabled())
               {
                  if (isReindexingSupport)
                  {
                     log.debug("RDBMS indexing enabled as the major version is 9, the minor version is 7 "
                        + "and the maintenance version is greater or equals to 2 knowing that the extracted value is "
                        + maintenanceVersion + ".");
                  }
                  else
                  {
                     log.debug("RDBMS indexing disabled as the major version is 9, the minor version is 7 "
                        + "and the maintenance version is lower than 2 knowing that the extracted value is "
                        + maintenanceVersion + ".");
                  }
               }
            }
            else
            {
               if (log.isDebugEnabled())
               {
                  log.debug("RDBMS indexing disabled as the major version is lower than 9 or the minor version is lower than 7.");
               }
               isReindexingSupport = false;
            }
         }
         catch (NumberFormatException e)
         {
            isReindexingSupport = false;
            log.error("Error checking product version.", e);
         }
         catch (RepositoryException e)
         {
            isReindexingSupport = false;
            log.error("Error checking product version.", e);
         }
         catch (SQLException e)
         {
            isReindexingSupport = false;
            log.error("Error checking product version.", e);
         }
         finally
         {
            if (con != null)
            {
               try
               {
                  con.close();
               }
               catch (SQLException e)
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
            }
         }

         if (!isReindexingSupport)
         {
            log.debug("The version of DB2 is prior to 9.7.2, so the old indexing mechanism will be used");
         }
      }

      return isReindexingSupport;
   }
}
