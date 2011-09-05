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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 23.03.2007
 * 
 * Access Oracle implicit connection caching and pooling stuff using reflection to prevent Maven
 * dependecies on ora drivers from POM.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: OracleConnectionFactory.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class OracleConnectionFactory extends GenericConnectionFactory
{

   public static int CONNCACHE_MAX_LIMIT = 25;

   public static int CONNCACHE_MIN_LIMIT = 2;

   public static int CONNCACHE_INACTIVITY_TIMEOUT = 3600;

   public static int CONNCACHE_ABADONDED_TIMEOUT = 1800;

   protected final Object ociDataSource;

   /**
    * OracleConnectionFactory constructor. For CLI interface ONLY!
    * 
    * @param dbDriver
    *          - JDBC Driver
    * @param dbUrl
    *          - JDBC URL
    * @param dbUserName
    *          - database username
    * @param dbPassword
    *          - database user password
    * @param containerName
    *          - Container name (see configuration)
    * @param multiDb
    *          - multidatabase state flag
    * @param valueStorageProvider
    *          - external Value Storages provider
    * @param maxBufferSize
    *          - Maximum buffer size (see configuration)
    * @param swapDirectory
    *          - Swap directory (see configuration)
    * @param swapCleaner
    *          - Swap cleaner (internal FileCleaner).
    * @throws RepositoryException
    *           if error occurs
    */
   public OracleConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner) throws RepositoryException
   {

      // ;D:\Devel\oracle_instantclient_10_2\;C:\oracle\ora92\bin;

      /*
       * ERROR: if no oci in path and oci url requested Error:
       * java.lang.reflect.InvocationTargetException java.lang.reflect.InvocationTargetException at
       * sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method) at
       * sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:39)
       * at
       * sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl
       * .java:27) at java.lang.reflect.Constructor.newInstance(Constructor.java:494) at
       * ocipool.ConnPoolAppl.main(ConnPoolAppl.java:58) Caused by: java.lang.UnsatisfiedLinkError: no
       * ocijdbc10 in java.library.path at java.lang.ClassLoader.loadLibrary(ClassLoader.java:1682)
       * --------------------------------------------------------------------------- ERROR: if thin
       * url used and trying obtain oci data source java.lang.reflect.InvocationTargetException at
       * sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method) at
       * sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:39)
       * at
       * sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl
       * .java:27) at java.lang.reflect.Constructor.newInstance(Constructor.java:494) at
       * ocipool.ConnPoolAppl.main(ConnPoolAppl.java:58) Caused by: java.lang.ClassCastException:
       * oracle.jdbc.driver.T4CConnection at
       * oracle.jdbc.pool.OracleOCIConnectionPool.createConnectionPool
       * (OracleOCIConnectionPool.java:893)
       */

      super(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);

      Object cds = null;
      try
      {
         Class cdsClass = OracleConnectionFactory.class.getClassLoader().loadClass("oracle.jdbc.pool.OracleDataSource");
         Constructor cdsConstructor = cdsClass.getConstructor(new Class[]{});
         cds = cdsConstructor.newInstance(new Object[]{});

         // set cache properties
         Properties prop = new java.util.Properties();
         prop.setProperty("InitialLimit", String.valueOf(CONNCACHE_MIN_LIMIT));
         prop.setProperty("MinLimit", String.valueOf(CONNCACHE_MIN_LIMIT));
         prop.setProperty("MaxLimit", String.valueOf(CONNCACHE_MAX_LIMIT));
         prop.setProperty("InactivityTimeout", String.valueOf(CONNCACHE_INACTIVITY_TIMEOUT));
         prop.setProperty("AbandonedConnectionTimeout", String.valueOf(CONNCACHE_ABADONDED_TIMEOUT));

         Method setURL = cds.getClass().getMethod("setURL", new Class[]{String.class});
         setURL.invoke(cds, new Object[]{this.dbUrl});

         Method setUser = cds.getClass().getMethod("setUser", new Class[]{String.class});
         setUser.invoke(cds, new Object[]{this.dbUserName});

         Method setPassword = cds.getClass().getMethod("setPassword", new Class[]{String.class});
         setPassword.invoke(cds, new Object[]{this.dbPassword});

         Method setConnectionCachingEnabled =
            cds.getClass().getMethod("setConnectionCachingEnabled", new Class[]{boolean.class});
         setConnectionCachingEnabled.invoke(cds, new Object[]{true});

         Method setConnectionCacheProperties =
            cds.getClass().getMethod("setConnectionCacheProperties", new Class[]{Properties.class});
         setConnectionCacheProperties.invoke(cds, new Object[]{prop});

         Method setConnectionCacheName = cds.getClass().getMethod("setConnectionCacheName", new Class[]{String.class});
         setConnectionCacheName.invoke(cds, new Object[]{"EXOJCR_OCI__" + containerName});

      }
      catch (Throwable e)
      {
         cds = null;
         String err = "Oracle OCI connection cache is unavailable due to error " + e;
         if (e.getCause() != null)
         {
            err += " (" + e.getCause() + ")";
         }
         err += ". Standard JDBC DriverManager will be used for connections opening.";
         if (log.isDebugEnabled())
            log.warn(err, e);
         else
            log.warn(err);
      }
      this.ociDataSource = cds; // actually instance of javax.sql.DataSource
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Connection getJdbcConnection(boolean readOnly) throws RepositoryException
   {
      if (ociDataSource != null)
         try
         {
            Connection conn = getCachedConnection();

            if (readOnly)
            {
               // set this feature only if it asked
               conn.setReadOnly(true);
            }

            return conn;
         }
         catch (Throwable e)
         {
            throw new RepositoryException("Oracle OCI cached connection open error " + e, e);
         }

      return super.getJdbcConnection(readOnly);
   }

   /**
    * Get CachedConnection.
    *
    * @return
    * @throws NoSuchMethodException
    * @throws IllegalArgumentException
    * @throws IllegalAccessException
    * @throws InvocationTargetException
    */
   protected Connection getCachedConnection() throws NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException
   {

      // NOTE: ociDataSource - actually instance of javax.sql.DataSource
      Method getConnection = ociDataSource.getClass().getMethod("getConnection", new Class[]{});
      Connection conn = (Connection)getConnection.invoke(ociDataSource, new Object[]{});

      return conn;
   }
}
