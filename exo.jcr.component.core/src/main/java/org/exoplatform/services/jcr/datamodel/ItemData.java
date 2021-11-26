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

package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS<br>
 *
 * The Item data object interface
 *
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ItemData.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Unsupported
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
