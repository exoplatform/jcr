/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.config;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;

import java.io.File;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: TesterRepositoryServiceConfigurationImpl.java 34360 16 Jul 2012 andrew.plotnikov $
 *
 */
public class TesterRepositoryServiceConfigurationImpl
{

   private File contentPath;

   private String configFileName;

   private RepositoryServiceConfigurationImpl repositoryServiceConf;

   /**
    * @param repositoryServiceConf
    * @throws RepositoryConfigurationException 
    */
   public TesterRepositoryServiceConfigurationImpl(RepositoryServiceConfigurationImpl repositoryServiceConf)
      throws RepositoryConfigurationException
   {
      this.repositoryServiceConf = repositoryServiceConf;
      if (this.repositoryServiceConf.param != null)
      {
         try
         {
            File configFile =
               new File(this.repositoryServiceConf.configurationService.getURL(
                  this.repositoryServiceConf.param.getValue()).toURI());
            contentPath = configFile.getParentFile();
            configFileName = configFile.getName();
         }
         catch (Exception e)
         {
            throw new RepositoryConfigurationException("Can't get content path");
         }
      }
   }

   /**
    * @return the configurationService
    */
   public File getContentPath()
   {
      return contentPath;
   }

   /**
    * @return the configFileName
    */
   public String getConfigFileName()
   {
      return configFileName;
   }

   public void retain() throws RepositoryException
   {
      repositoryServiceConf.retain();
   }

   public void setDefaultRepositoryName(String name)
   {
      this.repositoryServiceConf.setDefaultRepositoryName(name);
   }
}
