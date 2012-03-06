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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jdbc.DataSourceProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.loader.AdjListJDBCCacheLoaderConfig;
import org.jboss.cache.loader.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 */
public class JDBCConnectionFactory implements ConnectionFactory
{

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NonManagedConnectionFactory");

   private static final boolean trace = LOG.isTraceEnabled();

   static final ThreadLocal<Connection> connection = new ThreadLocal<Connection>();

   private DataSource dataSource;

   private String datasourceName;

   public void setConfig(AdjListJDBCCacheLoaderConfig config)
   {
      datasourceName = config.getDatasourceName();
   }

   private DataSourceProvider getDataSourceProvider()
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      if (container == null)
      {
         LOG.warn("The current container cannot be found which prevents to retrieve the DataSourceProvider");
         return null;
      }
      DataSourceProvider dsProvider =
         (DataSourceProvider)container.getComponentInstanceOfType(DataSourceProvider.class);
      if (dsProvider == null)
      {
         LOG.warn("The DataSourceProvider cannot be found in the container " + container.getContext().getName()
            + ", it will be considered as non managed ");
      }
      return dsProvider;
   }

   public void start() throws Exception
   {
      // A datasource will be registered in JNDI in the start portion of
      // its lifecycle, so now that we are in start() we can look it up

      DataSourceProvider dsProvider = getDataSourceProvider();
      InitialContext ctx = null;
      try
      {
         if (dsProvider == null)
         {
            ctx = new InitialContext();
            dataSource = (DataSource)ctx.lookup(datasourceName);
         }
         else
         {
            dataSource = dsProvider.getDataSource(datasourceName);
         }
         if (trace)
         {
            LOG.trace("Datasource lookup for " + datasourceName + " succeded: " + dataSource);
         }
      }
      catch (NamingException e)
      {
         reportAndRethrowError("Failed to lookup datasource " + datasourceName, e);
      }
      finally
      {
         if (ctx != null)
         {
            try
            {
               ctx.close();
            }
            catch (NamingException e)
            {
               LOG.warn("Failed to close naming context.", e);
            }
         }
      }
   }

   public void prepare(Object tx)
   {
      Connection con = getConnection();
      try
      {
         if (con.getAutoCommit())
         {
            con.setAutoCommit(false);
         }
      }
      catch (Exception e)
      {
         reportAndRethrowError("Failed to set auto-commit", e);
      }

      /* Connection set in ThreadLocal, no reason to return. It was previously returned for legacy purpouses
      and to trace log the connection opening in JDBCCacheLoader. */
      connection.set(con);

      if (trace)
      {
         LOG.trace("opened tx connection: tx=" + tx + ", con=" + con);
      }

   }

   public Connection getConnection()
   {
      Connection con = connection.get();

      if (con == null)
      {
         try
         {
            con = checkoutConnection();
            //               connection.set(con);
         }
         catch (SQLException e)
         {
            reportAndRethrowError("Failed to get connection for datasource=" + datasourceName, e);
         }
      }

      if (trace)
      {
         LOG.trace("using connection: " + con);
      }

      return con;
   }

   public Connection checkoutConnection() throws SQLException
   {
      return dataSource.getConnection();
   }

   public void commit(Object tx)
   {
      Connection con = connection.get();
      if (con == null)
      {
         throw new IllegalStateException("Failed to commit: thread is not associated with the connection!");
      }

      try
      {
         con.commit();
         if (trace)
         {
            LOG.trace("committed tx=" + tx + ", con=" + con);
         }
      }
      catch (SQLException e)
      {
         reportAndRethrowError("Failed to commit", e);
      }
      finally
      {
         closeTxConnection(con);
      }
   }

   public void rollback(Object tx)
   {
      Connection con = connection.get();

      try
      {
         con.rollback();
         if (trace)
         {
            LOG.trace("rolledback tx=" + tx + ", con=" + con);
         }
      }
      catch (SQLException e)
      {
         reportAndRethrowError("Failed to rollback", e);
      }
      finally
      {
         closeTxConnection(con);
      }
   }

   public void close(Connection con)
   {
      if (con != null && con != connection.get())
      {
         try
         {
            con.close();

            if (trace)
            {
               LOG.trace("closed non tx connection: " + con);
            }
         }
         catch (SQLException e)
         {
            LOG.warn("Failed to close connection " + con, e);
         }
      }
   }

   public void stop()
   {
   }

   private void closeTxConnection(Connection con)
   {
      safeClose(con);
      connection.set(null);
   }

   private void safeClose(Connection con)
   {
      if (con != null)
      {
         try
         {
            con.close();
         }
         catch (SQLException e)
         {
            LOG.warn("Failed to close connection", e);
         }
      }
   }

   private void reportAndRethrowError(String message, Exception cause) throws IllegalStateException
   {
      LOG.error(message, cause);
      throw new IllegalStateException(message, cause);
   }
}
