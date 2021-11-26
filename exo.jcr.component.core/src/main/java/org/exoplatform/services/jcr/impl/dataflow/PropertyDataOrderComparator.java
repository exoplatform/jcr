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

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Comparator;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version
 */
public class PropertyDataOrderComparator implements Comparator<PropertyData>
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.PropertyDataOrderComparator");

   public int compare(PropertyData p1, PropertyData p2)
   {
      int r = 0;

      InternalQName qname1 = p1.getQPath().getName();
      InternalQName qname2 = p2.getQPath().getName();
      if (qname1.equals(Constants.JCR_PRIMARYTYPE))
      {
         r = Integer.MIN_VALUE;
      }
      else if (qname2.equals(Constants.JCR_PRIMARYTYPE))
      {
         r = Integer.MAX_VALUE;
      }
      else if (qname1.equals(Constants.JCR_MIXINTYPES))
      {
         r = Integer.MIN_VALUE + 1;
      }
      else if (qname2.equals(Constants.JCR_MIXINTYPES))
      {
         r = Integer.MAX_VALUE - 1;
      }
      else if (qname1.equals(Constants.JCR_UUID))
      {
         r = Integer.MIN_VALUE + 2;
      }
      else if (qname2.equals(Constants.JCR_UUID))
      {
         r = Integer.MAX_VALUE - 2;
      }
      else
      {
         r = qname1.getAsString().compareTo(qname2.getAsString());
      }

      return r;
   }

}
