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

package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class RelatedDefinition<T>
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RelatedDefinition");

   private final T ancestorDefinition;

   private final T recepientDefinition;

   /**
    * @param ancestorDefinition
    * @param recepientDefinition
    */
   public RelatedDefinition(T ancestorDefinition, T recepientDefinition)
   {
      super();
      this.ancestorDefinition = ancestorDefinition;
      this.recepientDefinition = recepientDefinition;
   }

   public T getAncestorDefinition()
   {
      return ancestorDefinition;
   }

   public T getRecepientDefinition()
   {
      return recepientDefinition;
   }
}
