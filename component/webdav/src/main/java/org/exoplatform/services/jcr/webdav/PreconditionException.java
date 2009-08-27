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
package org.exoplatform.services.jcr.webdav;

/**
 * Created by The eXo Platform SARL .<br/>
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
