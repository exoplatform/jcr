/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.quota;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Accumulates changes from every saves during whole period. From time to time
 * all changes are pushed to coordinator to persist.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ChangesLog.java 34360 2009-07-22 23:58:59Z tolusha $
 */
class ChangesLog extends ConcurrentLinkedQueue<ChangesItem>
{
   /**
    * Returns workspace changed size accumulated during
    * some period.
    */
   public long getWorkspaceChangedSize()
   {
      long wsDelta = 0;

      Iterator<ChangesItem> changes = iterator();
      while (changes.hasNext())
      {
         wsDelta += changes.next().getWorkspaceChangedSize();
      }

      return wsDelta;
   }

   /**
    * Return changed size for particular node accumulated during
    * some period.
    */
   public long getNodeChangedSize(String nodePath)
   {
      long nodeDelta = 0;

      Iterator<ChangesItem> changes = iterator();
      while (changes.hasNext())
      {
         nodeDelta += changes.next().getNodeChangedSize(nodePath);
      }

      return nodeDelta;
   }

   /**
    * Merges all current existed changes into one single {@link ChangesItem} and return them.
    * Don't care if after merge new entries will come.
    */
   public ChangesItem pollAndMergeAll()
   {
      ChangesItem totalChanges = new ChangesItem();

      for (ChangesItem particularChanges = poll(); particularChanges != null; particularChanges = poll())
      {
         totalChanges.merge(particularChanges);
      }

      return totalChanges;
   }
}
