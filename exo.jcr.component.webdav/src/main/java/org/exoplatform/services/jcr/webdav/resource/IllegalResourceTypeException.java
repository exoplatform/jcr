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

package org.exoplatform.services.jcr.webdav.resource;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

@SuppressWarnings("serial")
public class IllegalResourceTypeException extends Exception
{

   /**
    * Default constructor.
    */
   public IllegalResourceTypeException()
   {
      super();
   }

   /**
    * Constructor accepting String as message and Throwable object as the second
    * argument.
    * 
    * @param message message
    * @param t Throwable
    */
   public IllegalResourceTypeException(String message, Throwable t)
   {
      super(message, t);
   }

   /**
    * Constructor accepting String as message.
    * 
    * @param message message
    */
   public IllegalResourceTypeException(String message)
   {
      super(message);
   }

   /**
    * Constructor accepting Throwable as an argument.
    * 
    * @param t Throwable
    */
   public IllegalResourceTypeException(Throwable t)
   {
      super(t);
   }

}
