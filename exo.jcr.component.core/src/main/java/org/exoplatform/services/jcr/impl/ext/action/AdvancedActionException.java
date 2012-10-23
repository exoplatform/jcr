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
