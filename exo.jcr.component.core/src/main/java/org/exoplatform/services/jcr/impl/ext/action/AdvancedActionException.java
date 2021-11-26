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

package org.exoplatform.services.jcr.impl.ext.action;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:dvishinskiy@exoplatform.com">Dmitriy Vyshinskiy</a>
 * @version $Id: $
 */
public class AdvancedActionException extends RepositoryException
{

   /**
    * {@inheritDoc}
    */
   public AdvancedActionException()
   {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public AdvancedActionException(String message)
   {
      super(message);
   }

   /**
    * {@inheritDoc}
    */
   public AdvancedActionException(Throwable rootCause)
   {
      super(rootCause);
   }

   /**
    * {@inheritDoc}
    */
   public AdvancedActionException(String message, Throwable rootCause)
   {
      super(message, rootCause);
   }

}
