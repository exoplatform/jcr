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

package org.exoplatform.services.jcr.impl.storage;

import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: SystemDataContainerHolder.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

/**
 * System DataContainer holder.
 * 
 * Used to store Container and provide it as dependency on statup time.
 */
public class SystemDataContainerHolder
{
   /**
    * Actual data Container.
    */
   private WorkspaceDataContainer dataContainer;

   /**
    * SystemDataContainerHolder constructor.
    * 
    * @param dataContainer
    *          - data Container instance
    */
   public SystemDataContainerHolder(WorkspaceDataContainer dataContainer)
   {
      this.dataContainer = dataContainer;
   }

   /**
    * Returns Container instance
    * 
    * @return WorkspaceDataContainer instance
    */
   public WorkspaceDataContainer getContainer()
   {
      return dataContainer;
   }
}
