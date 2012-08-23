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
package org.exoplatform.services.jcr.webdav;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.jcr.webdav.util.TextUtil;

import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: MimeTypeRecognizer.java 23.08.2012 dkuleshov $
 *
 */
public class MimeTypeRecognizer
{
   private final MimeTypeResolver mimeTypeResolver;

   private final MediaType mediaType;

   private final boolean untrustedAgent;

   private final String fileName;

   public MimeTypeRecognizer(String fileName, MimeTypeResolver mimeTypeResolver, MediaType mediaType,
      boolean trustedAgent)
   {
      this.mimeTypeResolver = mimeTypeResolver;
      this.mediaType = mediaType;
      this.untrustedAgent = trustedAgent;
      this.fileName = fileName;
   }

   public boolean isMimeTypeRecognized()
   {
      if (TextUtil.getExtension(fileName).isEmpty()
         && mimeTypeResolver.getMimeType(fileName).equals(mimeTypeResolver.getDefaultMimeType()))
      {
         return false;
      }
      
      return true;
   }

   public boolean isEncodingSet()
   {
      return mediaType != null && mediaType.getParameters().get("charset") != null;
   }
      

   public String getMimeType()
   {
      if (mediaType == null || untrustedAgent)
      {
         return mimeTypeResolver.getMimeType(fileName);
      }

      return mediaType.getType() + "/" + mediaType.getSubtype();
   }

   public String getEncoding()
   {
      return mediaType == null ? null : mediaType.getParameters().get("charset");
   }
}
