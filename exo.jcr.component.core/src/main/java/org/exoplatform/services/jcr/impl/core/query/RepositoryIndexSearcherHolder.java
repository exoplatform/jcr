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

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class RepositoryIndexSearcherHolder implements Startable
{
   private final List<SearchManager> indexSearchers;

   /**
    * @param indexSearchers
    */
   public RepositoryIndexSearcherHolder()
   {
      super();
      this.indexSearchers = new ArrayList<SearchManager>();
   }

   /**
    * @return the indexSearchers
    */
   public void addIndexSearcher(final SearchManager indexSearcher)
   {
      this.indexSearchers.add(indexSearcher);
   }

   /**
    * @return
    * @throws IndexException
    */
   public Set<String> getFieldNames() throws IndexException
   {
      final Set<String> fildsSet = new HashSet<String>();

      for (final SearchManager queryHandler : this.indexSearchers)
      {

         fildsSet.addAll(queryHandler.getFieldNames());
      }
      return fildsSet;
   }

   public Set<String> getNodesByNodeType(final InternalQName nodeType) throws RepositoryException
   {
      final Set<String> result = new HashSet<String>();
      for (final SearchManager indexingService : this.indexSearchers)
      {
         result.addAll(indexingService.getNodesByNodeType(nodeType));
      }

      return result;
   }

   /**
    * @param uri
    * @return
    * @throws RepositoryException
    */
   public Set<String> getNodesByUri(final String uri) throws RepositoryException
   {
      final Set<String> result = new HashSet<String>();
      for (final SearchManager indexingService : this.indexSearchers)
      {
         result.addAll(indexingService.getNodesByUri(uri));
      }

      return result;
   }

   /**
    * @return the indexSearchers
    */
   public void removeIndexSearcher(final SearchManager indexSearcher)
   {
      this.indexSearchers.remove(indexSearcher);
   }

   public void start()
   {
   }

   public void stop()
   {
      this.indexSearchers.clear();
   }
}
