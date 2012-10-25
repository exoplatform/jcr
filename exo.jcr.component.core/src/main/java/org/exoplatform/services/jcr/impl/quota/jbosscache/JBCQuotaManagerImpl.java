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
package org.exoplatform.services.jcr.impl.quota.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.Property;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.quota.BaseQuotaManager;
import org.exoplatform.services.jcr.impl.quota.QuotaManagerException;
import org.exoplatform.services.jcr.impl.quota.QuotaPersister;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.rpc.RPCService;

import java.util.Iterator;

import javax.jcr.RepositoryException;

/**
 * JBC implementation QuotamManager.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: JBCQuotaManagerImpl.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class JBCQuotaManagerImpl extends BaseQuotaManager
{
   // ------------------------------------------ jbosscache parameters names

   public static final String JBOSSCACHE_JDBC_CL_DATASOURCE = "jbosscache-cl-cache.jdbc.datasource";

   public static final String JBOSSCACHE_CONFIGURATION = "jbosscache-configuration";

   public static final String JBOSSCACHE_CLUSTER_NAME = "jbosscache-cluster-name";

   public static final String JGROUPS_CONFIGURATION = "jgroups-configuration";

   public static final String JBOSSCACHE_JDBC_TABLE_NAME = "jbosscache-cl-cache.jdbc.table.name";

   public static final String JBOSSCACHE_JDBC_TABLE_CREATE = "jbosscache-cl-cache.jdbc.table.create";

   public static final String JBOSSCACHE_JDBC_TABLE_DROP = "jbosscache-cl-cache.jdbc.table.drop";

   public static final String JBOSSCACHE_JDBC_TABLE_PRIMARY_KEY = "jbosscache-cl-cache.jdbc.table.primarykey";

   public static final String JBOSSCACHE_JDBC_CL_NODE_COLUMN_TYPE = "jbosscache-cl-cache.jdbc.node.type";

   public static final String JBOSSCACHE_JDBC_CL_NODE_COLUMN = "jbosscache-cl-cache.jdbc.node.column";

   public static final String JBOSSCACHE_JDBC_CL_FQN_COLUMN_TYPE = "jbosscache-cl-cache.jdbc.fqn.type";

   public static final String JBOSSCACHE_JDBC_CL_FQN_COLUMN = "jbosscache-cl-cache.jdbc.fqn.column";

   public static final String JBOSSCACHE_JDBC_CL_PARENT_COLUMN = "jbosscache-cl-cache.jdbc.parent.column";

   // ------------------------------------------ DefaultValues

   public static final String DEFAULT_JBOSSCACHE_JDBC_TABLE_NAME = "jcr_quota";

   public static final String DEFAULT_JBOSSCACHE_CLUSTER_NAME = "JCR-cluster-quota";

   public static final String DEFAULT_JGROUPS_CONFIGURATION = "udp-mux.xml";

   public static final String DEFAULT_JBOSSCACHE_JDBC_TABLE_CREATE = "true";

   public static final String DEFAULT_JBOSSCACHE_JDBC_TABLE_DROP = "false";

   public static final String DEFAULT_JBOSSCACHE_JDBC_TABLE_PRIMARY_KEY = "jcrquota_pk";

   public static final String DEFAULT_JBOSSCACHE_JDBC_CL_NODE_COLUMN_TYPE = "auto";

   public static final String DEFAULT_JBOSSCACHE_JDBC_CL_NODE_COLUMN = "node";

   public static final String DEFAULT_JBOSSCACHE_JDBC_CL_FQN_COLUMN_TYPE = "auto";

   public static final String DEFAULT_JBOSSCACHE_JDBC_CL_FQN_COLUMN = "fqn";

   public static final String DEFAULT_JBOSSCACHE_JDBC_CL_PARENT_COLUMN = "parent";

   /**
    * JBCQuotaManagerImpl constructor.
    */
   public JBCQuotaManagerImpl(InitParams initParams, RPCService rpcService, ConfigurationManager cfm,
      InitialContextInitializer contextInitializer) throws RepositoryConfigurationException, QuotaManagerException
   {
      super(initParams, rpcService, cfm, contextInitializer);
   }

   /**
    * JBCQuotaManagerImpl constructor.
    */
   public JBCQuotaManagerImpl(InitParams initParams, ConfigurationManager cfm,
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
         entry = prepareJBCParameters(initParams);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryConfigurationException(e.getMessage(), e);
      }

      return new JBCQuotaPersister(entry, cfm);
   }

   /**
    * Returns prepared {@link MappedParametrizedObjectEntry} instance with parameters
    * needed to create jboss cache and cache loader.
    */
   private MappedParametrizedObjectEntry prepareJBCParameters(InitParams initParams) throws RepositoryException
   {
      MappedParametrizedObjectEntry qmEntry = new QuotaManagerEntry();

      putDefaultValues(qmEntry);
      putConfiguredValues(initParams, qmEntry);

      ExoJBossCacheFactory.configureJDBCCacheLoader(qmEntry, JBOSSCACHE_JDBC_CL_DATASOURCE,
         JBOSSCACHE_JDBC_CL_NODE_COLUMN_TYPE, JBOSSCACHE_JDBC_CL_FQN_COLUMN_TYPE);

      return qmEntry;
   }

   private void putDefaultValues(MappedParametrizedObjectEntry qmEntry)
   {
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_TABLE_NAME, DEFAULT_JBOSSCACHE_JDBC_TABLE_NAME);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_TABLE_CREATE, DEFAULT_JBOSSCACHE_JDBC_TABLE_CREATE);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_TABLE_DROP, DEFAULT_JBOSSCACHE_JDBC_TABLE_DROP);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_TABLE_PRIMARY_KEY, DEFAULT_JBOSSCACHE_JDBC_TABLE_PRIMARY_KEY);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN_TYPE, DEFAULT_JBOSSCACHE_JDBC_CL_NODE_COLUMN_TYPE);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN, DEFAULT_JBOSSCACHE_JDBC_CL_NODE_COLUMN);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_CL_FQN_COLUMN_TYPE, DEFAULT_JBOSSCACHE_JDBC_CL_FQN_COLUMN_TYPE);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_CL_FQN_COLUMN, DEFAULT_JBOSSCACHE_JDBC_CL_FQN_COLUMN);
      qmEntry.putParameterValue(JBOSSCACHE_JDBC_CL_PARENT_COLUMN, DEFAULT_JBOSSCACHE_JDBC_CL_PARENT_COLUMN);

      qmEntry.putParameterValue(JBOSSCACHE_CLUSTER_NAME, DEFAULT_JBOSSCACHE_CLUSTER_NAME);
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
