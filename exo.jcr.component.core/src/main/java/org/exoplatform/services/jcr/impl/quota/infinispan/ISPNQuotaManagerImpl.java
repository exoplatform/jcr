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
package org.exoplatform.services.jcr.impl.quota.infinispan;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.Property;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.quota.BaseQuotaManager;
import org.exoplatform.services.jcr.impl.quota.QuotaManagerException;
import org.exoplatform.services.jcr.impl.quota.QuotaPersister;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.ManagedConnectionFactory;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.rpc.RPCService;

import java.util.Iterator;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ISPNQuotaManagerImpl.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ISPNQuotaManagerImpl extends BaseQuotaManager
{
   // ------------------------------------------ ISPN cache parameters names

   public static final String INFINISPAN_CLUSTER_NAME = "infinispan-cluster-name";

   public static final String JGROUPS_CONFIGURATION = "jgroups-configuration";

   public static final String INFINISPAN_JDBC_CL_DATASOURCE = "infinispan-cl-cache.jdbc.datasource";

   public static final String INFINISPAN_JDBC_CL_CONNECTION_FACTORY = "infinispan-cl-cache.jdbc.connectionFactory";

   public static final String INFINISPAN_CONFIGURATION = "infinispan-configuration";

   public static final String INFINISPAN_JDBC_TABLE_NAME = "infinispan-cl-cache.jdbc.table.name";

   public static final String INFINISPAN_JDBC_TABLE_CREATE = "infinispan-cl-cache.jdbc.table.create";

   public static final String INFINISPAN_JDBC_TABLE_DROP = "infinispan-cl-cache.jdbc.table.drop";

   public static final String INFINISPAN_JDBC_CL_DATA_COLUMN_TYPE = "infinispan-cl-cache.jdbc.data.type";

   public static final String INFINISPAN_JDBC_CL_DATA_COLUMN = "infinispan-cl-cache.jdbc.data.column";

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN_TYPE = "infinispan-cl-cache.jdbc.id.type";

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN = "infinispan-cl-cache.jdbc.id.column";

   public static final String INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN_TYPE = "infinispan-cl-cache.jdbc.timestamp.type";

   public static final String INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN = "infinispan-cl-cache.jdbc.timestamp.column";

   // ------------------------------------------ DefaultValues

   public static final String DEFAULT_INFINISPANE_CLUSTER_NAME = "JCR-cluster-quota";

   public static final String DEFAULT_JGROUPS_CONFIGURATION = "udp-mux.xml";

   public static final String DEFAULT_INFINISPAN_JDBC_TABLE_NAME = "jcr";

   public static final String DEFAULT_INFINISPAN_JDBC_TABLE_CREATE = "true";

   public static final String DEFAULT_INFINISPAN_JDBC_TABLE_DROP = "false";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_DATA_COLUMN_TYPE = "auto";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_DATA_COLUMN = "data";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_ID_COLUMN_TYPE = "auto";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_ID_COLUMN = "id";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN_TYPE = "auto";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN = "timestamp";

   public static final String DEFAULT_INFINISPAN_JDBC_CL_CONNECTION_FACTORY = ManagedConnectionFactory.class.getName();

   /**
    * ISPNQuotaManager constructor.
    */
   public ISPNQuotaManagerImpl(InitParams initParams, RPCService rpcService, ConfigurationManager cfm,
      InitialContextInitializer contextInitializer) throws RepositoryConfigurationException, QuotaManagerException
   {
      super(initParams, rpcService, cfm, contextInitializer);
   }

   /**
    * ISPNQuotaManager constructor.
    */
   public ISPNQuotaManagerImpl(InitParams initParams, ConfigurationManager cfm,
      InitialContextInitializer contextInitializer) throws RepositoryConfigurationException, QuotaManagerException
   {
      this(initParams, null, cfm, contextInitializer);
   }

   /**
    * {@inheritDoc}
    */
   protected QuotaPersister initQuotaPersister() throws RepositoryConfigurationException, QuotaManagerException
   {
      MappedParametrizedObjectEntry entry;
      try
      {
         entry = prepareISPNParameters(initParams);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryConfigurationException(e.getMessage(), e);
      }

      return new ISPNQuotaPersister(entry, cfm);
   }

   /**
    * Returns prepared {@link MappedParametrizedObjectEntry} instance with parameters
    * needed to create ISPN cache and cache store.
    */
   private MappedParametrizedObjectEntry prepareISPNParameters(InitParams initParams) throws RepositoryException
   {
      MappedParametrizedObjectEntry qmEntry = new QuotaManagerEntry();

      putDefaultValues(qmEntry);
      putConfiguredValues(initParams, qmEntry);

      ISPNCacheFactory.configureCacheStore(qmEntry, INFINISPAN_JDBC_CL_DATASOURCE,
         INFINISPAN_JDBC_CL_DATA_COLUMN_TYPE, INFINISPAN_JDBC_CL_ID_COLUMN_TYPE,
         INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN_TYPE);

      return qmEntry;
   }

   private void putDefaultValues(MappedParametrizedObjectEntry qmEntry)
   {
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_CONNECTION_FACTORY, DEFAULT_INFINISPAN_JDBC_CL_CONNECTION_FACTORY);
      qmEntry.putParameterValue(INFINISPAN_JDBC_TABLE_NAME, DEFAULT_INFINISPAN_JDBC_TABLE_NAME);
      qmEntry.putParameterValue(INFINISPAN_JDBC_TABLE_CREATE, DEFAULT_INFINISPAN_JDBC_TABLE_CREATE);
      qmEntry.putParameterValue(INFINISPAN_JDBC_TABLE_DROP, DEFAULT_INFINISPAN_JDBC_TABLE_DROP);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN_TYPE, DEFAULT_INFINISPAN_JDBC_CL_DATA_COLUMN_TYPE);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN, DEFAULT_INFINISPAN_JDBC_CL_DATA_COLUMN);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN_TYPE,
         DEFAULT_INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN_TYPE);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN, DEFAULT_INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_ID_COLUMN_TYPE, DEFAULT_INFINISPAN_JDBC_CL_ID_COLUMN_TYPE);
      qmEntry.putParameterValue(INFINISPAN_JDBC_CL_ID_COLUMN, DEFAULT_INFINISPAN_JDBC_CL_ID_COLUMN);

      qmEntry.putParameterValue(INFINISPAN_CLUSTER_NAME, DEFAULT_INFINISPANE_CLUSTER_NAME);
      qmEntry.putParameterValue(JGROUPS_CONFIGURATION, DEFAULT_JGROUPS_CONFIGURATION);
   }

   private void putConfiguredValues(InitParams initParams, MappedParametrizedObjectEntry qmEntry)
   {
      PropertiesParam props = initParams.getPropertiesParam(BaseQuotaManager.CACHE_CONFIGURATION_PROPERTIES_PARAM);
      for (Iterator<Property> iter = props.getPropertyIterator(); iter.hasNext();)
      {
         Property prop = iter.next();
         qmEntry.putParameterValue(prop.getName(), prop.getValue());
      }
   }

   private class QuotaManagerEntry extends MappedParametrizedObjectEntry
   {
   }
}
