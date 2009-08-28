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
package org.exoplatform.services.jcr.ext.repository;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.jcr.config.AbstractRepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.RepositoryEntry;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:vitaly.parfonov@gmail.com">Vitaly Parfonov</a>
 * @version $Id: $
 */
public class RepositoryServiceConf
   extends AbstractRepositoryServiceConfiguration
{

   /**
    * @param defaultRepositoryName 
    *          String, the default repository name
    * @param repositories
    *          List of RepositoryEntry-s
    */
   public RepositoryServiceConf(List<RepositoryEntry> repositories, String defaultRepositoryName)
   {
      this.defaultRepositoryName = defaultRepositoryName;
      this.repositoryConfigurations = repositories;
   }

   /**
    *  The empty constructor. 
    */
   public RepositoryServiceConf()
   {
      this.repositoryConfigurations = new ArrayList<RepositoryEntry>();
   }

   /**
    * @param repositories
    *          List of RepositoryEntry
    */
   public RepositoryServiceConf(List<RepositoryEntry> repositories)
   {
      this.repositoryConfigurations = repositories;
   }

   /**
    * @return List
    *           return the list of RepositoryEntry
    */
   public List<RepositoryEntry> getRepositories()
   {
      return repositoryConfigurations;
   }

   /**
    * @param repositories
    *          List of RepositoryEntry
    */
   public void setRepositories(List<RepositoryEntry> repositories)
   {
      this.repositoryConfigurations = repositories;
   }

   public RepositoryEntry getRepositoryEntry(String repositoryName)
   {
      for (RepositoryEntry repositoryEntry : this.repositoryConfigurations)
      {
         if (repositoryEntry.getName().equals(repositoryName))
            return repositoryEntry;
      }
      return null;
   }

}
