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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$
 */

public abstract class ValueStoragePlugin
{
   protected List<ValuePluginFilter> filters;

   protected String id;

   protected String repository;

   protected String workspace;

   /**
    * Initialize this plug-in. Used at start time.
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
    * Open a ValueIOChannel. Used in {@link ValueStoragePluginProvider.getApplicableChannel(PropertyData, int)} 
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
    * Get Storage Id.
    * 
    * @return String
    */
   public final String getId()
   {
      return id;
   }

   /**
    * Set Storage Id. Id can be set once only. 
    * 
    * @param id
    *          String
    */
   public final void setId(String id)
   {
      if (this.id == null)
      {
         this.id = id;
      }
   }

   /**
    * Gives the name of the repository that owns the value storage
    */
   protected final String getRepository()
   {
      return repository;
   }

   /**
    * Sets the name of the repository that owns the value storage
    */
   public final void setRepository(String repository)
   {
      if (this.repository == null)
         this.repository = repository;
   }

   /**
    * Gives the name of the workspace that owns the value storage
    */
   protected final String getWorkspace()
   {
      return workspace;
   }

   /**
    * Sets the name of the workspace that owns the value storage
    */
   public final void setWorkspace(String workspace)
   {
      if (this.workspace == null)
         this.workspace = workspace;
   }

   /**
    * In case the value storage supports the {@link URL}, this method
    * will provide the {@link ValueStorageURLConnection} managed by the value storage
    * corresponding to the given URL.
    * @throws IOException
    *           if an error occurs while creating the connection
    * @throws UnsupportedOperationException if {@link URL} are not supported by the {@link ValueStoragePlugin}
    */
   public ValueStorageURLConnection createURLConnection(URL u) throws IOException
   {
      return getURLStreamHandler().createURLConnection(u, repository, workspace, id);
   }

   /**
    * Runs the consistency check operation.
    * 
    * @param dataConnection
    *          - connection to metadata storage
    */
   public void checkConsistency(WorkspaceStorageConnection dataConnection)
   {
   }

   /**
    * Return true if this storage has same <code>storageId</code>.
    * 
    * @param storageId
    *          String
    * @return boolean, true if id matches
    */
   public boolean isSame(String storageId)
   {
      return getId().equals(storageId);
   }

   /**
    * Creates an {@link URL} corresponding to the given resource within the context of
    * the current {@link ValueStoragePlugin}
    * @param resourceId the id of the resource for which we want the corresponding URL
    * @return the URL corresponding to the given resource id
    * @throws MalformedURLException if the URL was not properly formed
    */
   public URL createURL(String resourceId) throws MalformedURLException
   {
      StringBuilder url = new StringBuilder(64);
      url.append(ValueStorageURLStreamHandler.PROTOCOL);
      url.append(":/");
      url.append(repository);
      url.append('/');
      url.append(workspace);
      url.append('/');
      url.append(id);
      url.append('/');
      url.append(resourceId);
      return new URL(null, url.toString(), getURLStreamHandler());
   }

   /**
    * Gives the {@link ValueStorageURLStreamHandler} corresponding to the current {@link ValueStoragePlugin}
    * @throws UnsupportedOperationException if {@link URL} are not supported by the {@link ValueStoragePlugin}
    */
   protected ValueStorageURLStreamHandler getURLStreamHandler()
   {
      throw new UnsupportedOperationException("The value storage " + repository + "/" + workspace + "/" + id + " doesn't support URL");
   }
}
