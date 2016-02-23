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
package org.exoplatform.jcr.backupconsole;

/**
 * Internal backup client exception wrapper.
 * <p>
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TransportException.java 111 2008-11-11 11:11:11Z serg $
 */
@SuppressWarnings("serial")
public class BackupExecuteException extends Exception
{

   /**
    * Constructor.
    * 
    * @param message exception message.
    * @param cause exception cause.
    */
   public BackupExecuteException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * Constructor.
    * 
    * @param message exception message.
    */
   public BackupExecuteException(String message)
   {
      super(message);
   }

}
