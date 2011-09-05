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
package org.exoplatform.services.jcr.ext.backup;

/**
 * Created by The eXo Platform SAS.
 *  Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 06.12.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupConfigurationException.java 568 2008-01-09 16:48:40Z rainf0x $
 */
public class BackupConfigurationException extends Exception
{
   /**
    * BackupConfigurationException  constructor.
    *
    * @param message
    *          String, the exception message 
    */
   public BackupConfigurationException(String message)
   {
      super(message);
   }

   /**
    * BackupConfigurationException  constructor.
    *
    * @param message
    *          String, the exception message
    * @param e
    *         Throwable, the cause exception
    */
   public BackupConfigurationException(String message, Throwable e)
   {
      super(message, e);
   }

   /**
    * BackupConfigurationException  constructor.
    *
    * @param e
    *          Throwable, the cause exception
    */
   public BackupConfigurationException(Throwable e)
   {
      super(e.getMessage(), e);
   }
}
