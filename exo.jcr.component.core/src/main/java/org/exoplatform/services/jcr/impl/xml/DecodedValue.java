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

import org.apache.ws.commons.util.Base64.Decoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
   private StringBuilder StringBuilder;

   /**
    * true if DecodedValue is completed
    */
   private boolean complete;

   /**
    * Indicates whether the value has been explicitly declared as encoded in Base64
    */
   private boolean binary;

   /**
    * Dafault constructor.
    */
   public DecodedValue()
   {
      super();
      StringBuilder = new StringBuilder();
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
         StringBuilder = null;
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
   public StringBuilder getStringBuffer()
   {
      return StringBuilder;
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
    * Used to determine the end of value tag in system view import.
    * 
    * @param complete
    */
   public void setComplete(boolean complete)
   {
      this.complete = complete;
   }

   /**
    * Used to determine the end of value tag in system view import.
    * 
    * @return
    */
   public boolean isComplete()
   {
      return complete;
   }

   /**
    * Indicates whether the value has been explicitly declared as encoded in Base64
    */
   public boolean isBinary()
   {
      return binary;
   }

   /**
    * Used to indicate if the value has been explicitly declared as encoded in Base64
    */
   public void setBinary(boolean binary)
   {
      this.binary = binary;
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

      return StringBuilder.toString();
   }
}
