/*
 * Copyright (C) 2011 eXo Platform SAS.
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
