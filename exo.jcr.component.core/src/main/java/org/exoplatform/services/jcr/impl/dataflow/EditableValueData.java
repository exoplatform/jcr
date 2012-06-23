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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.core.value.EditableBinaryValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

public class EditableValueData extends TransientValueData
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.EditableValueData");

   /**
    * EditableValueData constructor.
    */
   public EditableValueData(byte[] bytes, int orderNumber) throws IOException
   {
      this.delegate = new ByteArrayNewEditableValueData(orderNumber, bytes);
   }

   /**
    * EditableValueData constructor.
    */
   public EditableValueData(InputStream stream, int orderNumber, SpoolConfig spoolConfig) throws IOException
   {
      this.delegate = new StreamNewEditableValueData(stream, orderNumber, spoolConfig);
   }

   public void update(InputStream stream, long length, long position) throws IOException, RepositoryException
   {
      ((EditableBinaryValue)this.delegate).update(stream, length, position);
   }

   public void setLength(long size) throws IOException, RepositoryException
   {
      ((EditableBinaryValue)this.delegate).setLength(size);
   }
}
