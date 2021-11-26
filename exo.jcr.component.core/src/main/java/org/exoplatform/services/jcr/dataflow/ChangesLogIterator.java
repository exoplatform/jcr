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

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by The eXo Platform SAS.<br> iterator of PlainChangesLog
 * 
 * @author Gennady Azarenkov
 * @version $Id: ChangesLogIterator.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ChangesLogIterator
{

   private Iterator<PlainChangesLog> internalIterator;

   public ChangesLogIterator(Collection<PlainChangesLog> logs)
   {
      internalIterator = logs.iterator();
   }

   /**
    * @return if there is next changes log
    */
   public boolean hasNextLog()
   {
      return internalIterator.hasNext();
   }

   /**
    * @return next changes log
    */
   public PlainChangesLog nextLog()
   {
      return internalIterator.next();
   }

   /**
    * remove changes log
    */
   public void removeLog()
   {
      internalIterator.remove();
   }
}
