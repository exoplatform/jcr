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

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov </a>
 * @version $Id: RepositoryServiceConfiguration.java 2038 2005-10-05 16:50:11Z geaz $
 */

public class RepositoryServiceConfiguration extends AbstractRepositoryServiceConfiguration
{

   public final RepositoryEntry getRepositoryConfiguration(String name) throws RepositoryConfigurationException
   {
      for (int i = 0; i < getRepositoryConfigurations().size(); i++)
      {
         RepositoryEntry conf = (RepositoryEntry)getRepositoryConfigurations().get(i);
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
         e.printStackTrace();
         throw new RepositoryConfigurationException("Error in config initialization " + e);
      }
   }

   /**
    * Checks if current configuration can be saved.
    * 
    * @return
    */
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
