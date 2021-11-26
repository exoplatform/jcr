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

package org.exoplatform.services.jcr.config;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: RepositoryConfigurationException.java 11907 2008-03-13 15:36:21Z ksm $
 */

/**
 * This exception is thrown when condition occurred.
 */
public class RepositoryConfigurationException extends Exception
{
   /**
    * Constructs an Exception without a message.
    */
   public RepositoryConfigurationException()
   {
      super();
   }

   /**
    * Constructs an Exception with a detailed message.
    * 
    * @param message
    *          The message associated with the exception.
    */
   public RepositoryConfigurationException(String message)
   {
      super(message);
   }

   public RepositoryConfigurationException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * RepositoryConfigurationException  constructor.
    *
    * @param cause
    *         Throwable, the base exception.
    */
   public RepositoryConfigurationException(Throwable cause)
   {
      super(cause);
   }
}
