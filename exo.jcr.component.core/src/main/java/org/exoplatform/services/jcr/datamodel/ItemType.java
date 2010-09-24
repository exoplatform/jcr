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