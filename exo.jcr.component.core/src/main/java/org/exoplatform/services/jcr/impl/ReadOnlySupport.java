/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: ReadOnlySupport.java 34360 20.03.2012 andrew.plotnikov $
 *
 */
public interface ReadOnlySupport
{
   /**
    * Status of write-operations restrictions.
    * 
    * Read-only status is descriptive within the container, i.e. will not prevent any write
    * operation.
    * 
    * Used in DataManager implementations.
    * 
    * @return true - if write-operations allowed, false - otherwise.
    */
   boolean isReadOnly();

   /**
    * Set status of write-operations restrictions.
    * 
    * Read-only status is descriptive within the container, i.e. will not prevent any write
    * operation.
    * 
    * Used in DataManager implementations.
    * 
    * @param status
    *          , true - if write-operations allowed, false - otherwise.
    */
   void setReadOnly(boolean status);
}
