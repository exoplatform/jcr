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

package org.exoplatform.services.jcr.dataflow;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemDataVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface ItemDataVisitor
{

   /**
    * @return data manager wired data
    */
   ItemDataConsumer getDataManager();

   /**
    * visit propertyData
    * 
    * @param property
    * @throws RepositoryException
    */
   void visit(PropertyData property) throws RepositoryException;

   /**
    * visit NodeData
    * 
    * @param node
    * @throws RepositoryException
    */
   void visit(NodeData node) throws RepositoryException;

}
