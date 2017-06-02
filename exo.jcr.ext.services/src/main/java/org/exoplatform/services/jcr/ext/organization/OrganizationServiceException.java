/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.organization;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 22.08.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: OrganizationServiceException.java 33732 2009-07-08 15:00:43Z pnedonosko $
 */
public class OrganizationServiceException extends Exception
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = -4144453302333115329L;

   /**
    * OrganizationServiceException constructor.
    * 
    * @param message
    *         the detailed message
    * @param cause
    *          the cause
    */
   public OrganizationServiceException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * OrganizationServiceException constructor.
    * 
    * @param message
    *          the detailed message
    */
   public OrganizationServiceException(String message)
   {
      super(message);
   }

}
