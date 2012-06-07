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
package org.exoplatform.services.jcr.impl.storage.value;

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValuePluginFilter;
import org.exoplatform.services.jcr.storage.value.ValueStoragePlugin;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS. Per-workspace factory object for ValueStoragePlugin
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: StandaloneStoragePluginProvider.java 13463 2007-03-16 09:17:29Z geaz $
 */

public class StandaloneStoragePluginProvider extends ArrayList<ValueStoragePlugin> implements
   ValueStoragePluginProvider
{

   private static final long serialVersionUID = 4537116106932443262L;

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.StandaloneStoragePluginProvider");

   /**
    * Value storage enabling parameter. For interal usage only and testing purpose.
    */
   private static final String VALUE_STORAGE_ENABLED_PARAM = "enabled";

   /**
    * ValueData resorces holder (Files etc). It's singleton feature.
    */
   private final ValueDataResourceHolder resorcesHolder;

   public StandaloneStoragePluginProvider(WorkspaceEntry wsConfig) throws RepositoryConfigurationException, IOException
   {

      this.resorcesHolder = new ValueDataResourceHolder();

      List<ValueStorageEntry> storages = wsConfig.getContainer().getValueStorages();

      if (storages != null)
      {
         for (ValueStorageEntry storageEntry : storages)
         {
            if (storageEntry.getParameterBoolean(VALUE_STORAGE_ENABLED_PARAM, true))
            {
               // can be only one storage with given id
               for (ValueStoragePlugin vsp : this)
               {
                  if (vsp.getId().equals(storageEntry.getId()))
                  {
                     throw new RepositoryConfigurationException("Value storage with ID '" + storageEntry.getId()
                        + "' already exists");
                  }
               }

               Object o = null;
               try
               {
                  o = ClassLoading.forName(storageEntry.getType(), this).newInstance();
               }
               catch (InstantiationException e)
               {
                  log.error("Value Storage Plugin instantiation FAILED. ", e);
                  continue;
               }
               catch (IllegalArgumentException e)
               {
                  log.error("Value Storage Plugin instantiation FAILED. ", e);
                  continue;
               }
               catch (SecurityException e)
               {
                  log.error("Value Storage Plugin instantiation FAILED. ", e);
                  continue;
               }
               catch (IllegalAccessException e)
               {
                  log.error("Value Storage Plugin instantiation FAILED. ", e);
                  continue;
               }
               catch (ClassNotFoundException e)
               {
                  log.error("Value Storage Plugin instantiation FAILED. ", e);
                  continue;
               }
               if (!(o instanceof ValueStoragePlugin))
               {
                  log.error("Not a ValueStoragePlugin object IGNORED: " + o);
                  continue;
               }

               ValueStoragePlugin plugin = (ValueStoragePlugin)o;
               // init filters
               ArrayList<ValuePluginFilter> filters = new ArrayList<ValuePluginFilter>();
               List<ValueStorageFilterEntry> filterEntries = storageEntry.getFilters();
               for (ValueStorageFilterEntry filterEntry : filterEntries)
               {
                  ValuePluginFilter filter =
                     new ValuePluginFilter(PropertyType.valueFromName(filterEntry.getPropertyType()), null, null,
                        filterEntry.getMinValueSize());
                  filters.add(filter);
               }

               // init properties
               Properties props = new Properties();
               List<SimpleParameterEntry> paramEntries = storageEntry.getParameters();
               for (SimpleParameterEntry paramEntry : paramEntries)
               {
                  props.setProperty(paramEntry.getName(), paramEntry.getValue());
               }

               plugin.init(props, resorcesHolder);
               plugin.setId(storageEntry.getId());
               plugin.setFilters(filters);

               add(plugin);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public ValueIOChannel getApplicableChannel(PropertyData property, int valueOrderNumer) throws IOException
   {
      Iterator<ValueStoragePlugin> plugins = iterator();
      while (plugins.hasNext())
      {
         ValueStoragePlugin plugin = plugins.next();
         List<ValuePluginFilter> filters = plugin.getFilters();
         for (ValuePluginFilter filter : filters)
         {
            if (filter.match(property, valueOrderNumer))
            {
               return plugin.openIOChannel();
            }
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void checkConsistency(WorkspaceStorageConnection dataConnection)
   {
      Iterator<ValueStoragePlugin> plugins = iterator();
      while (plugins.hasNext())
      {
         ValueStoragePlugin plugin = plugins.next();
         plugin.checkConsistency(dataConnection);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ValueIOChannel getChannel(String storageId) throws IOException, ValueStorageNotFoundException
   {
      Iterator<ValueStoragePlugin> plugins = iterator();
      while (plugins.hasNext())
      {
         ValueStoragePlugin plugin = plugins.next();
         if (plugin.isSame(storageId))
         {
            return plugin.openIOChannel();
         }
      }
      throw new ValueStorageNotFoundException("No value storage found with id " + storageId);
   }
}
