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
package org.exoplatform.services.jcr.config;

import org.exoplatform.commons.utils.SecurityHelper;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.InputStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov </a>
 * @version $Id: RepositoryServiceConfiguration.java 2038 2005-10-05 16:50:11Z geaz $
 */

public class RepositoryServiceConfiguration extends AbstractRepositoryServiceConfiguration
{

   public RepositoryServiceConfiguration()
   {
   }

   public RepositoryServiceConfiguration(String defaultRepositoryName, List<RepositoryEntry> repositoryEntries)
   {
      this.defaultRepositoryName = defaultRepositoryName;
      this.repositoryConfigurations = repositoryEntries;
   }
   
   public final RepositoryEntry getRepositoryConfiguration(String name) throws RepositoryConfigurationException
   {
      for (int i = 0; i < getRepositoryConfigurations().size(); i++)
      {
         RepositoryEntry conf = getRepositoryConfigurations().get(i);
         if (conf.getName().equals(name))
            return conf;
      }
      throw new RepositoryConfigurationException("Repository not configured " + name);
   }

   protected final void init(InputStream is) throws RepositoryConfigurationException
   {
      try
      {
         IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         RepositoryServiceConfiguration conf = (RepositoryServiceConfiguration)uctx.unmarshalDocument(is, null);

         this.defaultRepositoryName = conf.getDefaultRepositoryName();
         this.repositoryConfigurations = conf.getRepositoryConfigurations();

         //      setDefaultRepositoryName(conf.getDefaultRepositoryName());
         //      getRepositoryConfigurations().clear();
         //      getRepositoryConfigurations().addAll(conf.getRepositoryConfigurations());

      }
      catch (JiBXException e)
      {
         throw new RepositoryConfigurationException("Error in config initialization " + e, e);
      }
   }

   protected final void merge(InputStream is) throws RepositoryConfigurationException
   {
      try
      {
         IBindingFactory factory;
         try
         {
            factory = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<IBindingFactory>()
            {
               public IBindingFactory run() throws Exception
               {
                  return BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof JiBXException)
            {
               throw (JiBXException)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }

         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         RepositoryServiceConfiguration conf = (RepositoryServiceConfiguration)uctx.unmarshalDocument(is, null);

         if (defaultRepositoryName == null)
         {
            this.defaultRepositoryName = conf.getDefaultRepositoryName();            
         }
         
         List<RepositoryEntry> repositoryEntries = conf.getRepositoryConfigurations();
         if (repositoryEntries == null || repositoryEntries.isEmpty())
         {
            return;            
         }
         if (repositoryConfigurations == null || repositoryConfigurations.isEmpty())
         {
            this.repositoryConfigurations = repositoryEntries;
            return;
         }
         Map<String, RepositoryEntry> mapRepoEntries = new LinkedHashMap<String, RepositoryEntry>();
         for (RepositoryEntry entry : repositoryConfigurations)
         {
            mapRepoEntries.put(entry.getName(), entry);
         }
         for (RepositoryEntry entry : repositoryEntries)
         {
            RepositoryEntry currentEntry = mapRepoEntries.get(entry.getName());
            if (currentEntry == null)
            {
               mapRepoEntries.put(entry.getName(), entry);
            }
            else
            {
               currentEntry.merge(entry);
            }
         }
         getRepositoryConfigurations().clear();
         getRepositoryConfigurations().addAll(mapRepoEntries.values());         
      }
      catch (JiBXException e)
      {
         throw new RepositoryConfigurationException("Error in config initialization " + e, e);
      }
   }
   
   /**
    * Checks if current configuration can be saved.
    * 
    * @return
    */
   @Override
   public boolean isRetainable()
   {
      return false;
   }

   /**
    * Saves current configuration to persistent.
    * 
    * @throws RepositoryException
    */
   public void retain() throws RepositoryException
   {
   }

}
