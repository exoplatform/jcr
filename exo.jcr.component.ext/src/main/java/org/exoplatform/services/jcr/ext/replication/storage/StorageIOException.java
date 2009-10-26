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
package org.exoplatform.services.jcr.ext.replication.storage;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 28.01.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CompositeIOException.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class StorageIOException extends IOException
{

   /**
    * The base cause exception.
    */
   protected final Throwable cause;

   /**
    * StorageIOException  constructor.
    *
    * @param message
    *          String, the exception message
    */
   public StorageIOException(String message)
   {
      super(message);
      this.cause = null;
   }

   /**
    * StorageIOException  constructor.
    *
    * @param message
    *          String, the exception message
    * @param cause
    *          Throwable, the cause exception
    */
   public StorageIOException(String message, Throwable cause)
   {
      super(message);
      this.cause = cause;
   }

   /**
    * {@inheritDoc}
    */
   public Throwable getCause()
   {
      return cause;
   }

}
