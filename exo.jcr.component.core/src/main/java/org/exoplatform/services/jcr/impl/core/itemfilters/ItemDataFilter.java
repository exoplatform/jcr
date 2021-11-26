/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.datamodel.ItemData;

import java.util.List;

public interface ItemDataFilter
{

   /**
    * Returns <code>true</code> if the specified element is to be included in the set of child
    * elements returbned by
    * 
    * @param item ItemData,
    *          The item to be tested for inclusion in the returned set.
    * @return a <code>boolean</code>.
    */
   boolean accept(ItemData item);

   List<? extends ItemData> accept(List<? extends ItemData> item);

}
