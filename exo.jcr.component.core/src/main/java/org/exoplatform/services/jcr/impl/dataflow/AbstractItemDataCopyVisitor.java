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

package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public abstract class AbstractItemDataCopyVisitor extends ItemDataTraversingVisitor
{

   public AbstractItemDataCopyVisitor(ItemDataConsumer dataManager, int maxLevel)
   {
      super(dataManager, maxLevel);
   }

   public AbstractItemDataCopyVisitor(ItemDataConsumer dataManager)
   {
      super(dataManager);
   }

   /**
    * Do actual copy of the property ValueDatas.
    * 
    * @param property  PropertyData
    * @return List of ValueData 
    * @throws RepositoryException if I/O error occurs
    */
   protected List<ValueData> copyValues(PropertyData property) throws RepositoryException
   {
      List<ValueData> src = property.getValues();
      List<ValueData> copy = new ArrayList<ValueData>(src.size());
      try
      {
         for (ValueData vd : src)
         {
            copy.add(ValueDataUtil.createTransientCopy(vd));
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Error of Value copy " + property.getQPath().getAsString(), e);
      }

      return copy;
   }

}
