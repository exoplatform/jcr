/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public abstract class AbstractRepositoryServiceConfiguration
{

   protected List<RepositoryEntry> repositoryConfigurations = new CopyOnWriteArrayList<RepositoryEntry>();

   protected String defaultRepositoryName;

   /**
    * Set default repository name
    * 
    * @param defaultRepositoryName
    */
   public void setDefaultRepositoryName(String defaultRepositoryName)
   {
      this.defaultRepositoryName = defaultRepositoryName;
   }

   /**
    * 
    * Get default repository name
    * 
    * @return
    */
   public final String getDefaultRepositoryName()
   {
      return defaultRepositoryName;
   }

   public List<RepositoryEntry> getRepositoryConfigurations()
   {
      return repositoryConfigurations;
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

}
