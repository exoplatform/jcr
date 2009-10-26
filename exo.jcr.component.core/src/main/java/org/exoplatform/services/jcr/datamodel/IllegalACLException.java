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
package org.exoplatform.services.jcr.datamodel;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 29.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: IllegalACLException.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class IllegalACLException extends Exception
{

   /**
    * IllegalACLException constructor.
    * 
    */
   public IllegalACLException()
   {
      super();
   }

   /**
    * IllegalACLException constructor.
    * 
    * @param message
    *          - error message
    * @param cause
    *          - error casue
    */
   public IllegalACLException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * IllegalACLException constructor.
    * 
    * @param message
    *          - error message
    */
   public IllegalACLException(String message)
   {
      super(message);
   }

   /**
    * IllegalACLException constructor.
    * 
    * @param cause
    *          - error casue
    */
   public IllegalACLException(Throwable cause)
   {
      super(cause);
   }

}
