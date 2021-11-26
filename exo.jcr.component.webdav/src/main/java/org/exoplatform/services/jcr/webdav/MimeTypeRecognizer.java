/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.jcr.webdav.util.TextUtil;

import javax.ws.rs.core.MediaType;

/**
 * Provides means to recognize mime-type information of the content 
 * (including mime-type itself and encoding)
 * 
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

   /**
    * Constructor
    * 
    * @param fileName - short name of the resource
    * @param mimeTypeResolver - provides means to resolve mime-type 
    * @param mediaType - media type instance (stores mime-type and encoding)
    * @param untrustedAgent - shows if agent to provide resource and it's mime type is listed as trusted
    * (no mime-type change is allowed for untrusted agents)
    */
   public MimeTypeRecognizer(String fileName, MimeTypeResolver mimeTypeResolver, MediaType mediaType,
      boolean untrustedAgent)
   {
      this.mimeTypeResolver = mimeTypeResolver;
      this.mediaType = mediaType;
      this.untrustedAgent = untrustedAgent;
      this.fileName = fileName;
   }

   /**
    * Shows if mime-type is recognized by {@link MimeTypeResolver}.
    */
   public boolean isMimeTypeRecognized()
   {
      return !TextUtil.getExtension(fileName).isEmpty();
   }

   /**
    * Shows if encoding is set via {@link MediaType}.
    */
   public boolean isEncodingSet()
   {
      return !untrustedAgent && mediaType != null && mediaType.getParameters().get("charset") != null;
   }
      

   /**
    * Returns mime-type of a resource according to {@link MediaType}
    * or {@link MimeTypeResolver} information.
    */
   public String getMimeType()
   {
      if (mediaType == null || untrustedAgent)
      {
         return mimeTypeResolver.getMimeType(fileName);
      }

      return mediaType.getType() + "/" + mediaType.getSubtype();
   }

   /**
    * Returns encoding according to {@link MediaType} or <code>null</code>
    * no encoding set or {@link MediaType} is no available. 
    */
   public String getEncoding()
   {
      if (mediaType == null || untrustedAgent)
      {
         return null;
      }

      return mediaType.getParameters().get("charset");
   }
}