/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchuk</a>
 * @version $Id: AbstractRecoveryFilter.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public abstract class AbstractRecoveryFilter
{

   protected SearchIndex searchIndex;
   
   protected QueryHandlerContext context;

   /**
    * Default constructor accepting searchIndex instance
    * @param searchIndex
    */
   public AbstractRecoveryFilter(SearchIndex searchIndex)
   {
      this.searchIndex = searchIndex;
      this.context = searchIndex.getContext();
   }

   /**
    * Frees resources associated with current filter
    */
   public void close()
   {

   }

   /**
    * @return true if index should be re-retrieved on JCR start
    */
   public abstract boolean accept() throws RepositoryException;

}
