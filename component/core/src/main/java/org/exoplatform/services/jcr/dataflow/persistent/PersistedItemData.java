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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;

/**
 * Created by The eXo Platform SAS. </br>
 * 
 * Immutable ItemData from persistent storage
 * 
 * @author Gennady Azarenkov
 * @version $Id: PersistedItemData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class PersistedItemData
   implements ItemData
{

   protected final String id;

   protected final QPath qpath;

   protected final String parentId;

   protected final int version;

   public PersistedItemData(String id, QPath qpath, String parentId, int version)
   {
      this.id = id;
      this.qpath = qpath;
      this.parentId = parentId;
      this.version = version;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getQPath()
    */
   public QPath getQPath()
   {
      return qpath;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getIdentifier()
    */
   public String getIdentifier()
   {
      return id;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getPersistedVersion()
    */
   public int getPersistedVersion()
   {
      return version;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getParentIdentifier()
    */
   public String getParentIdentifier()
   {
      return parentId;
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;

      if (obj == null)
         return false;

      if (obj instanceof ItemData)
      {
         return getIdentifier().hashCode() == ((ItemData) obj).getIdentifier().hashCode();
      }

      return false;
   }
}
