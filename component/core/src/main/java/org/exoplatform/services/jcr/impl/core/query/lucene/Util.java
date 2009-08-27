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

import java.io.IOException;
import java.util.Enumeration;

import org.exoplatform.services.log.Log;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.exoplatform.services.log.ExoLogger;

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
    * Disposes the document <code>old</code>. Closes any potentially open readers held by the
    * document.
    * 
    * @param old
    *          the document to dispose.
    */
   public static void disposeDocument(Document old)
   {
      for (Enumeration e = old.fields(); e.hasMoreElements();)
      {
         Field f = (Field) e.nextElement();
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
    * Returns <code>true</code> if the document is ready to be added to the index. That is all text
    * extractors have finished their work.
    * 
    * @param doc
    *          the document to check.
    * @return <code>true</code> if the document is ready; <code>false</code> otherwise.
    */
   public static boolean isDocumentReady(Document doc)
   {
      return true;
   }
}
