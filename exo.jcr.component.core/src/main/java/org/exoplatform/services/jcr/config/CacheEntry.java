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

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: CacheEntry.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class CacheEntry extends ExtendedMappedParametrizedObjectEntry
{
   public static final String CACHE = "cache";

   private boolean enabled;

   public CacheEntry()
   {
      super(CACHE);
   }

   public CacheEntry(List<SimpleParameterEntry> params)
   {
      super("org.exoplatform.services.jcr.impl.storage.cache.WorkspaceCache", params, CACHE);
   }

   /**
    * @return Returns the enabled.
    */
   public boolean isEnabled()
   {
      return enabled;
   }

   /**
    * @return Returns the enabled.
    */
   public boolean getEnabled()
   {
      return enabled;
   }

   /**
    * @param enabled
    *          The enabled to set.
    */
   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }
}
