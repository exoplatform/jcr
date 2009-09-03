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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.Enumeration;

import javax.jcr.PropertyType;

/**
 * <code>Util</code> provides various static utility methods.
 */
public class Util
{

   /**
    * The logger instance for this class.
    */
   private static final Log log = ExoLogger.getLogger(Util.class);

   /**
    * Disposes the document <code>old</code>. Closes any potentially open readers
    * held by the document.
    * 
    * @param old the document to dispose.
    */
   public static void disposeDocument(Document old)
   {
      for (Enumeration e = old.fields(); e.hasMoreElements();)
      {
         Field f = (Field)e.nextElement();
         if (f.readerValue() != null)
         {
            try
            {
               f.readerValue().close();
            }
            catch (IOException ex)
            {
               log.warn("Exception while disposing index document: " + ex);
            }
         }
      }
   }

   /**
    * Returns <code>true</code> if the document is ready to be added to the
    * index. That is all text extractors have finished their work.
    * 
    * @param doc the document to check.
    * @return <code>true</code> if the document is ready; <code>false</code>
    *         otherwise.
    */
   public static boolean isDocumentReady(Document doc)
   {
      return true;
   }

   /**
    * Depending on the type of the <code>reader</code> this method either closes
    * or releases the reader. The reader is released if it implements
    * {@link ReleaseableIndexReader}.
    * 
    * @param reader the index reader to close or release.
    * @throws IOException if an error occurs while closing or releasing the index
    *           reader.
    */
   public static void closeOrRelease(IndexReader reader) throws IOException
   {
      if (reader instanceof ReleaseableIndexReader)
      {
         ((ReleaseableIndexReader)reader).release();
      }
      else
      {
         reader.close();
      }
   }

   /**
    * Returns length of the internal value.
    *
    * @param value a value.
    * @return the length of the internal value or <code>-1</code> if the length
    *         cannot be determined.
    */
   public static long getLength(ValueData value, int propertyType)
   {
      if (propertyType == PropertyType.NAME || propertyType == PropertyType.PATH)
      {
         return -1;
      }
      else
      {
         return value.getLength();
      }
   }
}
