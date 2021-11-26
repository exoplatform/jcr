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

package org.exoplatform.services.jcr.webdav;

/**
 * Created by The eXo Platform SAS .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class Depth
{

   /**
    * String constant for depth "infinity" value.
    */
   public static final String INFINITY_NAME = "Infinity";

   /**
    * Integer constant for depth "infinity" value.
    */
   public static final int INFINITY_VALUE = -1;

   /**
    * Integer depth-value property.
    */
   private int intValue;

   /**
    * String depth-value property.
    */
   private String stringValue;

   /**
    * Creates a Depth object from the String.
    * 
    * @param strValue depth string value
    * @throws PreconditionException when some problems occurs.
    */
   public Depth(String strValue) throws PreconditionException
   {
      if (strValue == null || strValue.equalsIgnoreCase(INFINITY_NAME))
      {
         this.intValue = INFINITY_VALUE;
         this.stringValue = INFINITY_NAME;
      }
      else
      {
         try
         {
            this.intValue = new Integer(strValue);

            if ((this.intValue != 1) && (this.intValue != 0) && (this.intValue != INFINITY_VALUE))
            {
               throw new PreconditionException("Invalid depth value " + strValue);
            }

            this.stringValue = strValue;
         }
         catch (NumberFormatException e)
         {
            throw new PreconditionException("Invalid depth value " + strValue, e);
         }
      }
   }

   /**
    * Returns depth integer value.
    * 
    * @return depth int value
    */
   public int getIntValue()
   {
      return intValue;
   }

   /**
    * Returns depth String value.
    * 
    * @return depth String value
    */
   public String getStringValue()
   {
      return stringValue;
   }
}
