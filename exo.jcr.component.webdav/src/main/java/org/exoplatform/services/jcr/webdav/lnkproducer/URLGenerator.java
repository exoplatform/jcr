/*
 * Copyright (C) 2015 eXo Platform SAS.
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
package org.exoplatform.services.jcr.webdav.lnkproducer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:aboughzela@gmail.com">Aymen Boughzela</a>
 */

public class URLGenerator
{
   private static int[] urlHeader = {
      // [InternetShortcut]
      0x5B, 0x49, 0x6E, 0x74,
      0x65, 0x72, 0x6E, 0x65,
      0x74, 0x53, 0x68, 0x6F,
      0x72, 0x74, 0x63, 0x75,
      0x74, 0x5D, 0x0D, 0x0A,
      //URL=
      0x55, 0x52, 0x4C, 0x3D
   };

   /**
    * Servlet path.
    */
   private String servletPath;

   /**
    * Traget path.
    */
   private String targetPath;

   /**
    * Constructor.
    *
    * @param servletPath servlet path
    * @param targetPath target path
    */
   public URLGenerator(String servletPath, String targetPath)
   {
      this.servletPath = servletPath;
      this.targetPath = targetPath;
   }

   /**
    * Generates the content of url.
    *
    * @return url content
    * @throws java.io.IOException {@link java.io.IOException}
    */
   public byte[] generateLinkContent() throws IOException
   {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();

      // URL HEADER
      for (int i = 0; i < urlHeader.length; i++)
      {
         byte curByteValue = (byte)urlHeader[i];
         outStream.write(curByteValue);
      }
      String url = servletPath + "/" + targetPath;
      outStream.write(url.getBytes());

      return outStream.toByteArray();
   }

}
