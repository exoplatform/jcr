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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.datamodel.QPath;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 19.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JCRPathMatcher.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class JCRPathMatcher
{

   private QPath knownPath = null;

   private boolean forDescendants = false;

   private boolean forAncestors = false;

   public JCRPathMatcher(QPath knownPath, boolean forDescendants, boolean forAncestors)
   {
      this.knownPath = knownPath;
      this.forDescendants = forDescendants;
      this.forAncestors = forAncestors;
   }

   public boolean match(QPath path)
   {

      // any, e.g. *
      if (forDescendants && forAncestors && knownPath == null)
         return true;

      // descendants, e.g. /item/*
      if (forDescendants && knownPath != null)
      {
         return path.isDescendantOf(knownPath);
      }

      // ancestors, e.g. */item/
      if (forDescendants && knownPath != null)
      {
         return knownPath.isDescendantOf(path);
      }

      // descendants or ancestors, e.g. */item/*
      if (forDescendants && forAncestors && knownPath != null)
      {
         return path.isDescendantOf(knownPath) && knownPath.isDescendantOf(path);
      }

      return knownPath.equals(path);
   }

}
