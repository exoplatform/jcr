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
package org.exoplatform.services.jcr.impl.xml.importing.dataflow;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.xml.DecodedValue;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: PropertyInfo.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class PropertyInfo
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.PropertyInfo");

   /**
    * 
    */
   private InternalQName name;

   /**
    * 
    */
   private List<DecodedValue> values = new ArrayList<DecodedValue>();

   /**
    * 
    */
   private int type;

   /**
    * 
    */
   private String indentifer;

   /**
    * @return the indentifer
    */
   public String getIndentifer()
   {
      return indentifer;
   }

   /**
    * @return the name
    */
   public InternalQName getName()
   {
      return name;
   }

   /**
    * @return the type
    */
   public int getType()
   {
      return type;
   }

   /**
    * @return the values
    */
   public List<DecodedValue> getValues()
   {
      return values;
   }

   /**
    * 
    * @return - values count.
    */
   public int getValuesSize()
   {
      if (values != null)
         return values.size();
      return 0;
   }

   /**
    * @param indentifer
    *          the indentifer to set
    */
   public void setIndentifer(String indentifer)
   {
      this.indentifer = indentifer;
   }

   /**
    * @param name
    *          the name to set
    */
   public void setName(InternalQName name)
   {
      this.name = name;
   }

   /**
    * @param type
    *          the type to set
    */
   public void setType(int type)
   {
      this.type = type;
   }

   /**
    * @param values
    *          the values to set
    */
   public void setValues(List<DecodedValue> values)
   {
      this.values = values;
   }
}
