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
package org.exoplatform.services.jcr.core;

import java.io.InputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ExtendedProperty.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Platform
 */
public interface ExtendedProperty extends Property
{

   /**
    * Write binary data portion to the property value data.
    * 
    * @param index
    *          - value index, 0 for first-in-multivalue/single-value, 1 - second etc.
    * @param value
    *          - stream with the data portion
    * @param length
    *          - value bytes count will be written
    * @param position
    *          - position in the property value data from which the value will be written
    */
   void updateValue(int index, InputStream value, long length, long position) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException;

}
