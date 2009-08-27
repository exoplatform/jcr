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
package org.exoplatform.services.jcr.impl.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ws.commons.util.Base64.Decoder;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: DecodedValue.java 11987 2008-03-17 09:06:06Z ksm $
 */

/**
 * Temporary class for swapping values and decode binary values during import.
 * 
 * @author ksm
 */
public class DecodedValue
{
   /**
    * Decoder buffer.
    */
   private BufferedDecoder decoder;

   /**
    * String buffer.
    */
   private StringBuffer stringBuffer;

   /**
    * Dafault constructor.
    */
   public DecodedValue()
   {
      super();
      stringBuffer = new StringBuffer();
   }

   /**
    * @return Base64 decoder. It is write decoded incoming data into the temporary file.
    * @exception IOException
    *              if an I/O error occurs.
    */
   public Decoder getBinaryDecoder() throws IOException
   {
      if (decoder == null)
      {
         decoder = new BufferedDecoder();
         stringBuffer = null;
      }
      return decoder;
   }

   /**
    * @return InputStream from decoded file.
    * @exception IOException
    *              if an I/O error occurs.
    */
   public InputStream getInputStream() throws IOException
   {
      if (decoder == null)
      {
         return new ByteArrayInputStream(new byte[0]);
      }

      return decoder.getInputStream();
   }

   /**
    * @return String buffer.
    */
   public StringBuffer getStringBuffer()
   {
      return stringBuffer;
   }

   /**
    * Removes all temporary variables and files.
    * 
    * @throws IOException
    *           if file can't be removed.
    */
   public void remove() throws IOException
   {

      if (decoder != null)
      {
         decoder.remove();
         decoder = null;
      }
   }

   /**
    * @return string representation for value.
    */
   public String toString()
   {
      if (decoder != null)
      {
         return decoder.toString();
      }

      return stringBuffer.toString();
   }
}
