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

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 16.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: OraclePoolConnectionFactory.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class OraclePoolConnectionFactory extends GenericConnectionFactory
{

   public static int CONNPOOL_MAX_LIMIT = 20;

   public static int CONNPOOL_MIN_LIMIT = 2;

   public static int CONNPOOL_INCREMENT = 1;

   protected final Object ociPool;

   /**
    * OraclePoolConnectionFactory constructor. For CLI interface ONLY!
    */
   public OraclePoolConnectionFactory(JDBCDataContainerConfig containerConfig) throws RepositoryException
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

      super(containerConfig);

      Object cpool = null;
      try
      {
         Class<?> cpoolClass = ClassLoading.loadClass("oracle.jdbc.pool.OracleOCIConnectionPool", this);
         Constructor<?> cpoolConstructor =
            cpoolClass.getConstructor(new Class[]{String.class, String.class, String.class, Properties.class});

         cpool =
            cpoolConstructor.newInstance(new Object[]{this.containerConfig.dbUserName, this.containerConfig.dbPassword,
               this.containerConfig.dbUrl, null});
         Method setConnectionCachingEnabled =
            cpool.getClass().getMethod("setConnectionCachingEnabled", new Class[]{boolean.class});
         setConnectionCachingEnabled.invoke(cpool, new Object[]{true});
      }
      catch (Throwable e)
      {
         cpool = null;
         StringBuilder err = new StringBuilder("Oracle OCI connection pool is unavailable due to error ").append(e);
         if (e.getCause() != null)
         {
            err.append(" (").append(e.getCause()).append(")");
         }
         err.append(". Standard JDBC DriverManager will be used for connections opening.");
         if (log.isDebugEnabled())
         {
            log.warn(err, e);
         }
         else
         {
            log.warn(err);
         }
      }
      this.ociPool = cpool;

      // configure using CONNPOOL_MAX_LIMIT, CONNPOOL_MIN_LIMIT, CONNPOOL_INCREMENT
      try
      {
         reconfigure();
         displayPoolConfig();
      }
      catch (Throwable e)
      {
         if (log.isDebugEnabled())
         {
            log.warn("Oracle OCI connection pool configuration error " + e, e);
         }
         else
         {
            log.warn("Oracle OCI connection pool configuration error " + e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Connection getJdbcConnection(boolean readOnly) throws RepositoryException
   {
      if (ociPool != null)
      {
         try
         {
            Connection conn = getPoolConnection();

            if (readOnly)
            {
               // set this feature only if it asked
               conn.setReadOnly(true);
            }

            return conn;
         }
         catch (Throwable e)
         {
            throw new RepositoryException("Oracle OCI pool connection open error " + e, e);
         }
      }

      return super.getJdbcConnection(readOnly);
   }

   /**
    * getPoolConnection.
    *
    * @return
    * @throws NoSuchMethodException
    * @throws IllegalArgumentException
    * @throws IllegalAccessException
    * @throws InvocationTargetException
    */
   protected Connection getPoolConnection() throws NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException
   {
      Method getConnection = ociPool.getClass().getMethod("getConnection", new Class[]{});
      return (Connection)getConnection.invoke(ociPool, new Object[]{});
   }

   protected void reconfigure() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
      InvocationTargetException, NoSuchFieldException
   {
      if (ociPool != null)
      {
         // Set up the initial pool configuration
         Properties p1 = new Properties();
         String minLimitName = (String)ociPool.getClass().getField("CONNPOOL_MIN_LIMIT").get(null);
         String maxLimitName = (String)ociPool.getClass().getField("CONNPOOL_MAX_LIMIT").get(null);
         String incrName = (String)ociPool.getClass().getField("CONNPOOL_INCREMENT").get(null);

         p1.put(minLimitName, Integer.toString(CONNPOOL_MIN_LIMIT));
         p1.put(maxLimitName, Integer.toString(CONNPOOL_MAX_LIMIT));
         p1.put(incrName, Integer.toString(CONNPOOL_INCREMENT));

         // Enable the initial configuration
         ociPool.getClass().getMethod("setPoolConfig", new Class[]{Properties.class}).invoke(ociPool, new Object[]{p1});
      }
   }

   /**
    * Display the current status of the OracleOCIConnectionPool.
    */
   protected void displayPoolConfig() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
      InvocationTargetException
   {
      if (ociPool != null)
      {

         log.info(" =========== Oracle OCI connection pool config =========== ");

         log.info(" Min poolsize Limit:\t"
            + ociPool.getClass().getMethod("getMinLimit", new Class[]{}).invoke(ociPool, new Object[]{}));

         log.info(" Max poolsize Limit:\t"
            + ociPool.getClass().getMethod("getMaxLimit", new Class[]{}).invoke(ociPool, new Object[]{}));

         log.info(" PoolSize:\t\t\t"
            + ociPool.getClass().getMethod("getPoolSize", new Class[]{}).invoke(ociPool, new Object[]{}));

         log.info(" ActiveSize:\t\t"
            + ociPool.getClass().getMethod("getActiveSize", new Class[]{}).invoke(ociPool, new Object[]{}));
      }
   }

}
