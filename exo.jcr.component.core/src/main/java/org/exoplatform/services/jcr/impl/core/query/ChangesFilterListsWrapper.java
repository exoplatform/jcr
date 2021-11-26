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

import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;

import java.io.Serializable;
import java.util.Set;

/**
 * FOR TESTING PURPOSES ONLY. Used to avoid batching usage in indexer cache.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: ChangesFilterListsWrapper.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class ChangesFilterListsWrapper implements Serializable
{
   private static final long serialVersionUID = 1L;

   private Set<String> addedNodes;

   private Set<String> removedNodes;

   private Set<String> parentAddedNodes;

   private Set<String> parentRemovedNodes;

   private ChangesHolder changes;

   private ChangesHolder parentChanges;
   
   /**
    * Creates ChangesFilterListsWrapper data class, containing given lists.
    * 
    * @param addedNodes
    * @param removedNodes
    * @param parentAddedNodes
    * @param parentRemovedNodes
    */
   public ChangesFilterListsWrapper(Set<String> addedNodes, Set<String> removedNodes, Set<String> parentAddedNodes,
      Set<String> parentRemovedNodes)
   {
      this.addedNodes = addedNodes;
      this.removedNodes = removedNodes;
      this.parentAddedNodes = parentAddedNodes;
      this.parentRemovedNodes = parentRemovedNodes;
   }
   
   /**
    * Creates ChangesFilterListsWrapper data class, containing given lists.
    */
   public ChangesFilterListsWrapper(ChangesHolder changes, ChangesHolder parentChanges)
   {
      this.changes = changes;
      this.parentChanges = parentChanges;
   }

   public boolean withChanges()
   {
      return changes != null || parentChanges != null;
   }
   
   public ChangesHolder getChanges()
   {
      return changes;
   }

   public ChangesHolder getParentChanges()
   {
      return parentChanges;
   }
   
   public Set<String> getAddedNodes()
   {
      return addedNodes;
   }

   public Set<String> getRemovedNodes()
   {
      return removedNodes;
   }

   public Set<String> getParentAddedNodes()
   {
      return parentAddedNodes;
   }

   public Set<String> getParentRemovedNodes()
   {
      return parentRemovedNodes;
   }
}
