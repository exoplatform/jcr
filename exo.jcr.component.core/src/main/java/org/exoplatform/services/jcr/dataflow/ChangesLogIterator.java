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
