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
package org.exoplatform.services.jcr.impl.storage.jdbc.backup;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: BackupException 34360 2009-07-22 23:58:59Z tolusha $
 */
public class BackupException extends Exception
{
   /**
    * Constructor BackupException.
    * 
    * @param message
    *          the message
    */
   public BackupException(String message)
   {
      super(message);
   }

   /**
    * Constructor BackupException.
    * 
    * @param cause
    *          the cause
    */
   public BackupException(Throwable cause)
   {
      super(cause);
   }

   /**
    * Constructor BackupException.
    * 
    * @param message
    *          the message 
    * @param cause
    *          the cause
    */
   public BackupException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
