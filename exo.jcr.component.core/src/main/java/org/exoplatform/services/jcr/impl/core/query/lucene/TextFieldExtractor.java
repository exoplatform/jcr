/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Field;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.Reader;

public class TextFieldExtractor extends AbstractField
{

   /**
    * The serial version UID.
    */
   private static final long serialVersionUID = -2707986404659820071L;

   /**
    * The logger instance for this class.
    */
   private static final Log log              = ExoLogger.getLogger("exo.jcr.component.core.TextFieldExtractor");

   /**
    * The reader from where to read the text extract.
    */
   private final Reader reader;

   /**
    * The extract as obtained lazily from {@link #reader}.
    */
   volatile private String extract;

   /**
    * Creates a new <code>TextFieldExtractor</code> with the given
    * <code>name</code>.
    *
    * @param name the name of the field.
    * @param reader the reader where to obtain the string from.
    * @param store when set <code>true</code> the string value is stored in the
    *          index.
    * @param withOffsets when set <code>true</code> a term vector with offsets
    *          is written into the index.
    */
   public TextFieldExtractor(String name, Reader reader, boolean store, boolean withOffsets)
   {
      super(name, store ? Field.Store.YES : Field.Store.NO, Field.Index.ANALYZED, withOffsets
         ? Field.TermVector.WITH_OFFSETS : Field.TermVector.NO);
      this.reader = reader;
   }

   /**
    * @return the string value of this field.
    */
   public String stringValue()
   {
      if (extract == null)
      {
         synchronized (this)
         {
            if (extract == null)
            {
               StringBuilder textExtract = new StringBuilder();
               char[] buffer = new char[1024];
               int len;
               try
               {
                  while ((len = reader.read(buffer)) > -1)
                  {
                     textExtract.append(buffer, 0, len);
                  }
               }
               catch (IOException e)
               {
                  log.warn("Exception reading value for field: " + e.getMessage());
                  log.debug("Dump:", e);
               }
               finally
               {
                  try
                  {
                     reader.close();
                  }
                  catch (IOException e)
                  {
                     log.error(e.getLocalizedMessage(), e);
                  }
               }
               extract = textExtract.toString();
            }
         }
      }
      return extract;
   }

   /**
    * @return always <code>null</code>.
    */
   public Reader readerValue()
   {
      return null;
   }

   /**
    * @return always <code>null</code>.
    */
   public byte[] binaryValue()
   {
      return null;
   }

   /**
    * @return always <code>null</code>.
    */
   public TokenStream tokenStreamValue()
   {
      return null;
   }

   /**
    * Disposes this field and closes the underlying reader.
    *
    * @throws IOException if an error occurs while closing the reader.
    */
   public void dispose() throws IOException
   {
      reader.close();
   }
}
