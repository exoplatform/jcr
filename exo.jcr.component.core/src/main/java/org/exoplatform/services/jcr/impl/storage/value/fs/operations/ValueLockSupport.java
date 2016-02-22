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
package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 08.04.2009
 *
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a> 
 * @version $Id: ValueLockSupport.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public interface ValueLockSupport
{

   /**
    * Perform lock.
    *
    * @throws IOException if error occurs
    */
   void lock() throws IOException;

   /**
    * Perform unlock.
    *
    * @throws IOException  if error occurs
    */
   void unlock() throws IOException;

   /**
    * Share this lock resource with another lock.
    *
    * @param anotherLock ValueLockSupport
    * @throws IOException if error occurs
    */
   void share(ValueLockSupport anotherLock) throws IOException;

}
