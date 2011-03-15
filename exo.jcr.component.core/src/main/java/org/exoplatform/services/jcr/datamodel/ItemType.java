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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.datamodel;


/**
 * Class contains enumerated item types. Is used in methods to indicate what need exactly get: node or property.
 */
public enum ItemType {
   UNKNOWN, NODE, PROPERTY;

   /**
    * Indicate if item type suit for ItemData.  
    * 
    * @param itemData
    *          ItemData
    * @return true if item type is UNKNOWN type or the same as ItemData and false in other case 
    */
   public boolean isSuitableFor(ItemData itemData)
   {
      boolean isNode = itemData.isNode();
      return this == UNKNOWN || this == NODE && isNode || this == PROPERTY && !isNode;
   }

   /**
    * Return item type based on ItemData.
    * 
    * @param itemData
    *          item data
    * @return ItemType
    */
   public static ItemType getItemType(ItemData itemData)
   {
      return itemData.isNode() ? ItemType.NODE : ItemType.PROPERTY;
   }
}