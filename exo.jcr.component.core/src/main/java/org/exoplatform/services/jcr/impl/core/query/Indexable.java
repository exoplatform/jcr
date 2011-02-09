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
package org.exoplatform.services.jcr.impl.core.query;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: Indexable.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public interface Indexable
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
    * Indicates if component support extracting data from storage using paging.
    * 
    * @return boolean
    */
   boolean isPagingSupport();

}
