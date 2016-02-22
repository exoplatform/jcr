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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.datamodel.QPathEntry;

/**
 * Ordinary QPathEntry do not allows index equal to -1. Such index will be replaced with 1.
 * So PatternQPathEntry allows any index number. And Index -1 means that PatternQPathEntry cowers
 * all samename siblings.  
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: PatternQPathEntry.java 111 12.05.2011 serg $
 */
public class PatternQPathEntry extends QPathEntry
{
   private final int index;

   public PatternQPathEntry(String namespace, String name)
   {
      this(namespace, name, -1);
   }

   public PatternQPathEntry(String namespace, String name, int index)
   {
      super(namespace, name, index);
      this.index = index;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getIndex()
   {
      return this.index;
   }
}
