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

package org.exoplatform.services.jcr.impl.storage.value;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ValueDataNotFoundException.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ValueDataNotFoundException extends RepositoryException
{

   private static final long serialVersionUID = -3480032427540892483L;

   public ValueDataNotFoundException()
   {
      super();
   }

   public ValueDataNotFoundException(String message, Throwable rootCause)
   {
      super(message, rootCause);
   }

   public ValueDataNotFoundException(String message)
   {
      super(message);
   }

   public ValueDataNotFoundException(Throwable rootCause)
   {
      super(rootCause);
   }

}
