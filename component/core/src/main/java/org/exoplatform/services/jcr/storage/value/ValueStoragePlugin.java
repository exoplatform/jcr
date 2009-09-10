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
package org.exoplatform.services.jcr.storage.value;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueStoragePlugin.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class ValueStoragePlugin
{

   protected List<ValuePluginFilter> filters;

   protected String id;

   /**
    * Initialize this plugin. Used at start time.
    * 
    * @param props
    *          configuration Properties
    * @param resources
    *          ValueDataResourceHolder
    * @throws RepositoryConfigurationException
    *           if config error
    * @throws IOException
    *           if IO error
    */
   public abstract void init(Properties props, ValueDataResourceHolder resources)
      throws RepositoryConfigurationException, IOException;

   /**
    * Open ValueIOChannel. Used in {@link ValueStoragePluginProvider.getApplicableChannel(PropertyData, int)} 
    * and {@link ValueStoragePluginProvider.getChannel(String)}.
    * 
    * @return ValueIOChannel channel
    * @throws IOException
    *           if error occurs
    */
   public abstract ValueIOChannel openIOChannel() throws IOException;

   /**
    * Return filters.
    * 
    * @return List of ValuePluginFilter
    */
   public final List<ValuePluginFilter> getFilters()
   {
      return filters;
   }

   /**
    * Set filters.
    * 
    * @param filters
    *          List of ValuePluginFilter
    */
   public final void setFilters(List<ValuePluginFilter> filters)
   {
      this.filters = filters;
   }

   /**
    * Get Stirage Id.
    * 
    * @return String
    */
   public final String getId()
   {
      return id;
   }

   /**
    * Set Storage Id.
    * 
    * @param id
    *          String
    */
   public final void setId(String id)
   {
      this.id = id;
   }

   /**
    * Run consistency check operation.
    * 
    * @param dataConnection
    *          - connection to metadata storage
    */
   public abstract void checkConsistency(WorkspaceStorageConnection dataConnection);

   /**
    * Return true if this storage has same <code>storageId</code>.
    * 
    * @param storageId
    *          String
    * @return boolean, true if id matches
    */
   public abstract boolean isSame(String storageId);

}
