/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.backup;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: RestoreConfigurationException.java 111 2010-11-11 11:11:11Z rainf0x $
 */
public class RestoreConfigurationException
   extends Exception
{
   /**
    * RestoreConfigurationException  constructor.
    *
    * @param message
    *          String, the exception message 
    */
   public RestoreConfigurationException(String message)
   {
      super(message);
   }

   /**
    * RestoreConfigurationException  constructor.
    *
    * @param message
    *          String, the exception message
    * @param e
    *         Throwable, the cause exception
    */
   public RestoreConfigurationException(String message, Throwable e)
   {
      super(message, e);
   }

   /**
    * RestoreConfigurationException  constructor.
    *
    * @param e
    *          Throwable, the cause exception
    */
   public RestoreConfigurationException(Throwable e)
   {
      super(e.getMessage(), e);
   }
}

