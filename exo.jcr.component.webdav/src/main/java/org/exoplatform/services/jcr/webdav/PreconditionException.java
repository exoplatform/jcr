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
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

@SuppressWarnings("serial")
public class PreconditionException extends Exception
{

   /**
    * Default public constructor.
    */
   public PreconditionException()
   {
      super();
   }

   /**
    * Public constructor with specified message and Throwable.
    * 
    * @param message exception message
    * @param t Throwable
    */
   public PreconditionException(String message, Throwable t)
   {
      super(message, t);
   }

   /**
    * Public constructor with specified message.
    * 
    * @param message exception message
    */
   public PreconditionException(String message)
   {
      super(message);
   }

   /**
    * Public constructor with specified Throwable.
    * 
    * @param t Throwable
    */
   public PreconditionException(Throwable t)
   {
      super(t);
   }

}
