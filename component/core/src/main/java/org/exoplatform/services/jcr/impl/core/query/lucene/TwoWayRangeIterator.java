/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import javax.jcr.RangeIterator;

/**
 * Created by The eXo Platform SAS. Extends <code>ScoreNodeIterator</code> with the
 * <code>skipBack</code> methods.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public interface TwoWayRangeIterator
   extends RangeIterator
{
   /**
    * Skip a number of elements in the iterator.
    * 
    * @param skipNum
    *          the non-negative number of elements to skip
    * @throws java.util.NoSuchElementException
    *           if skipped past the first element in the iterator.
    */
   public void skipBack(long skipNum);

}
