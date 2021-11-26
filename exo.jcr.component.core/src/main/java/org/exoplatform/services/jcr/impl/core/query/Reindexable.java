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

package org.exoplatform.services.jcr.impl.core.query;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: Reindexable.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public interface Reindexable
{
   /**
    * Returns NodeDataIndexingIterator.
    * 
    * @param pageSize 
    *          the maximum amount of the rows which can be retrieved from storage per once
    * @return NodeDataIndexingIterator
    * @throws RepositoryException
    */
   NodeDataIndexingIterator getNodeDataIndexingIterator(int pageSize) throws RepositoryException;

   /**
    * Indicates if the component supports extracting data from storage using paging.
    * 
    * @return <code>true</code> if it is supported, <code>false</code> otherwise.
    */
   boolean isReindexingSupported();

   /**
    * Return nodes count.
    * 
    * @return
    *       nodes count
    * @throws RepositoryException
    */
   Long getNodesCount() throws RepositoryException;
}
