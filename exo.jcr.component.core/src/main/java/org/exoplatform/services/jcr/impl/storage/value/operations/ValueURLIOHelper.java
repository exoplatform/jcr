/*
 * Copyright (C) 2014 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.value.operations;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.storage.value.ValueStoragePlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Helper class offering the main methods that a value storage non file based could need
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class ValueURLIOHelper
{
   private ValueURLIOHelper() {}

   /**
    * Extracts the content of the given {@link ValueData} and links the data to 
    * the {@link URL} in the Value Storage if needed
    * @param plugin the plug-in that will manage the storage of the provided {@link ValueData}
    * @param value the value from which we want to extract the content
    * @param resourceId the internal id of the {@link ValueData}
    * @param spoolContent Indicates whether or not the content should always be spooled
    * @return the content of the {@link ValueData}
    * @throws IOException if the content could not be extracted
    */
   public static InputStream getContent(ValueStoragePlugin plugin, ValueData value, String resourceId, boolean spoolContent)
      throws IOException
   {
      if (value.isByteArray())
      {
         return new ByteArrayInputStream(value.getAsByteArray());
      }
      else if (value instanceof StreamPersistedValueData)
      {
         StreamPersistedValueData streamed = (StreamPersistedValueData)value;
         if (!streamed.isPersisted())
         {
            InputStream stream;
            // the Value not yet persisted, i.e. or in client stream or spooled to a temp file
            File tempFile;
            if ((tempFile = streamed.getTempFile()) != null)
            {
               // it's spooled Value, try move its file to VS
               stream = new FileInputStream(tempFile);
            }
            else
            {
               // not spooled, use client InputStream
               stream = streamed.getStream();
            }
            // link this Value to URL in VS
            InputStream result = streamed.setPersistedURL(plugin.createURL(resourceId), spoolContent);
            return result != null ? result : stream;
         }
      }
      return value.getAsStream();
   }
}
