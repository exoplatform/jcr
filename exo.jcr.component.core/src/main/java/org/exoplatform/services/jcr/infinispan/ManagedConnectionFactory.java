/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jdbc.DataSourceProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactoryConfig;
import org.infinispan.transaction.xa.GlobalTransaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Connection factory that can be used when on managed environments, like application servers. It knows how to look into
 * the JNDI tree at a certain location (configurable) and delegate connection management to the DataSource. In order to
 * enable it one should set the following two properties in any Jdbc cache store:
 * <pre>
 *   {@code
 *    <property name="connectionFactoryClass" value="org.exoplatform.services.jcr.infinispan.ManagedConnectionFactory"/>
 *    <property name="datasourceJndiLocation" value="java:/ManagedConnectionFactoryTest/DS"/>
 *   }
 * </pre>
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ManagedConnectionFactory extends ConnectionFactory
{

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ManagedConnectionFactory");

   private static final boolean trace = LOG.isTraceEnabled();

   private DataSource dataSource;

   private String datasourceName;

   /**
    * Indicates whether or not the datasource provide managed connections.
    */
   private boolean managed;

   static final ThreadLocal<Connection> connection = new ThreadLocal<Connection>();

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

   @Override
   public void start(ConnectionFactoryConfig config, ClassLoader classLoader) throws CacheLoaderException
   {
      InitialContext ctx = null;
      DataSourceProvider dsProvider = getDataSourceProvider();
      this.datasourceName = config.getDatasourceJndiLocation();
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
            managed = dsProvider.isManaged(datasourceName);
         }
         if (trace)
         {
            LOG.trace("Datasource lookup for {} succeeded: {} of type {}", datasourceName, dataSource, managed);
         }
         if (dataSource == null)
         {
            throw new CacheLoaderException(String.format("Could not find a connection in jndi under the name '%s'",
               datasourceName));
         }
      }
      catch (NamingException e)
      {
         throw new CacheLoaderException(e);
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
               LOG.debug("Could not close the context: " + e.getMessage());
            }
         }
      }
   }

   @Override
   public void stop()
   {
   }

   public void prepare(GlobalTransaction tx) throws CacheLoaderException
   {
      Connection con = getConnection(false);

      /* Connection set in ThreadLocal, no reason to return. It was previously returned for legacy purpouses
      and to trace log the connection opening in JDBC Cache Store. */
      connection.set(con);

      if (trace)
      {
         LOG.trace("opened tx connection: tx=" + tx + ", con=" + con);
      }

   }
 
   @Override
   public Connection getConnection() throws CacheLoaderException
   {
      return getConnection(true);
   }

   private Connection getConnection(boolean autoCommit) throws CacheLoaderException
   {
      Connection con = connection.get();

      if (con == null)
      {
         con = checkoutConnection();
         try
         {
            if (con.getAutoCommit() != autoCommit)
            {
               con.setAutoCommit(autoCommit);
            }
         }
         catch (Exception e)
         {
            reportAndRethrowError("Failed to set auto-commit to " + autoCommit, e);
         }
      }

      if (trace)
      {
         LOG.trace("using connection: " + con);
      }

      return con;
   }

   private Connection checkoutConnection() throws CacheLoaderException
   {
      Connection connection;
      try
      {
         connection = dataSource.getConnection();
      }
      catch (SQLException e)
      {
         throw new CacheLoaderException("Failed to get connection for datasource=" + datasourceName, e);
      }
      if (trace)
      {
         LOG.trace("Connection checked out: {}", connection);
      }
      return connection;
   }

   public void commit(GlobalTransaction tx)
   {
      Connection con = connection.get();
      if (con == null)
      {
         if (trace)
         {
            LOG.trace("There is nothing to commit.");
         }
         return;
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

   public void rollback(GlobalTransaction tx)
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

   @Override
   public void releaseConnection(Connection con)
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