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
package org.exoplatform.services.jcr.datamodel;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ItemData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface ItemData
{

   /**
    * @return QPath of this item
    */

   QPath getQPath();

   /**
    * @return identifier
    */
   String getIdentifier();

   /**
    * @return number of item version retrieved from container. If item is not persisted returns -1;
    */
   int getPersistedVersion();

   /**
    * @return parent NodeData. Parent is initialized on demand. It is possible that the method return
    *         null for root node only (but not neccessary)
    * @throws IllegalStateException
    *           if parent can not be initialized (for example was deleted by other session)
    */
   String getParentIdentifier();

   /**
    * @return if item data is node data
    */
   boolean isNode();

   /**
    * Accept visitor
    * 
    * @param visitor
    * @throws RepositoryException
    */
   void accept(ItemDataVisitor visitor) throws RepositoryException;

}
