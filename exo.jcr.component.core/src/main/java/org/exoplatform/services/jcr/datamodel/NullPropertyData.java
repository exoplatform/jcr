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

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: NullPropertyData.java 111 2011-05-30 11:11:11Z serg $
 */
public class NullPropertyData extends NullItemData implements PropertyData
{

   public NullPropertyData(NodeData parent, QPathEntry name)
   {
      super(parent, name);
   }

   public NullPropertyData(String id)
   {
      super(id);
   }

   public NullPropertyData()
   {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public int getType()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public List<ValueData> getValues()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMultiValued()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return false;
   }

}
