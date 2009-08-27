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

import java.util.ArrayList;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: CacheEntry.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class CacheEntry
   extends MappedParametrizedObjectEntry
{

   private boolean enabled;

   public CacheEntry()
   {
      super();
   }

   public CacheEntry(ArrayList params)
   {
      super("org.exoplatform.services.jcr.impl.storage.cache.WorkspaceCache", params);
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
