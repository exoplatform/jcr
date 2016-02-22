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

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id: IndexingTree.java 790 2009-11-20 13:45:40Z skabashnyuk $
 */
public class IndexingTree
{
   private final QPath indexingRootQpath;

   private final NodeData indexingRoot;

   private final QPath excludedPath;

   /**
    * Indicates if need to indexing every node. 
    */
   private final boolean allIndexing;

   /**
    * @param indexingRoot
    * @param excludedPath
    */
   public IndexingTree(NodeData indexingRoot, QPath excludedPath)
   {
      this.indexingRoot = indexingRoot;
      this.indexingRootQpath = indexingRoot.getQPath();
      this.excludedPath = excludedPath;
      this.allIndexing = indexingRoot.getQPath().getDepth() == 0;
   }

   /**
    * @return the indexingRoot
    */
   public NodeData getIndexingRoot()
   {
      return indexingRoot;
   }

   /**
    * Checks if the given event should be excluded based on the
    * {@link #excludedPath} setting.
    * 
    * @param event
    *            observation event
    * @return <code>true</code> if the event should be excluded,
    *         <code>false</code> otherwise
    */
   public boolean isExcluded(ItemState event)
   {
      return isExcluded(event.getData());
   }

   /**
    * Checks if the given event should be excluded based on the
    * {@link #excludedPath} setting.
    * 
    * @param eventData
    *            observation event
    * @return <code>true</code> if the event should be excluded,
    *         <code>false</code> otherwise
    */
   public boolean isExcluded(ItemData eventData)
   {
      if (excludedPath != null
         && (eventData.getQPath().isDescendantOf(excludedPath) || eventData.getQPath().equals(excludedPath)))
      {
         return true;
      }

      return !allIndexing && !eventData.getQPath().isDescendantOf(indexingRootQpath)
         && !eventData.getQPath().equals(indexingRootQpath);
   }
}
