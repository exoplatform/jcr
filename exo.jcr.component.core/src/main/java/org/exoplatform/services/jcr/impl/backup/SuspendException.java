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

package org.exoplatform.services.jcr.impl.backup;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: SuspendException.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SuspendException extends Exception
{
   /**
    * Constructor SuspendException.
    * 
    * @param cause
    *          the cause
    */
   public SuspendException(Throwable cause)
   {
      super(cause);
   }

   /**
    * Constructor SuspendException.
    * 
    * @param message
    *          the message
    */
   public SuspendException(String message)
   {
      super(message);
   }

   /**
    * Constructor SuspendException.
    * 
    * @param message
    *          the message
    * @param cause
    *          the cause
    */
   public SuspendException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
