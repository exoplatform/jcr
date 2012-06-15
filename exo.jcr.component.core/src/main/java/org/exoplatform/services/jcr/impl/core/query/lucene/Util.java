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
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * <code>Util</code> provides various static utility methods.
 */
public class Util
{

   /**
    * The logger instance for this class.
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.Util");

   /**
    * Disposes the document <code>old</code>. Closes any potentially open
    * readers held by the document.
    *
    * @param old the document to dispose.
    */
   public static void disposeDocument(final Document old)
   {
      for (Iterator it = old.getFields().iterator(); it.hasNext();)
      {
         Fieldable f = (Fieldable)it.next();
         try
         {
            if (f.readerValue() != null)
            {
               f.readerValue().close();
            }
            else if (f instanceof TextFieldExtractor)
            {
               TextFieldExtractor field = (TextFieldExtractor)f;
               field.dispose();
            }
         }
         catch (IOException ex)
         {
            log.warn("Exception while disposing index document: " + ex);
         }
      }
   }

   /**
    * Depending on the index format this method returns a query that matches
    * all nodes that have a property with a given <code>name</code>.
    *
    * @param name    the property name.
    * @param version the index format version.
    * @return Query that matches all nodes that have a property with the given
    *         <code>name</code>.
    */
   public static Query createMatchAllQuery(String name, IndexFormatVersion version)
   {
      if (version.getVersion() >= IndexFormatVersion.V2.getVersion())
      {
         // new index format style
         return new JcrTermQuery(new Term(FieldNames.PROPERTIES_SET, name));
      }
      else
      {
         return new MatchAllQuery(name);
      }
   }

   /**
    * Creates an {@link IOException} with <code>t</code> as its cause.
    *
    * @param t the cause.
    */
   public static IOException createIOException(Throwable t)
   {
      IOException ex = new IOException(t.getMessage());
      ex.initCause(t);
      return ex;
   }

   /**
    * Depending on the type of the <code>reader</code> this method either
    * closes or releases the reader. The reader is released if it implements
    * {@link ReleaseableIndexReader}.
    *
    * @param reader the index reader to close or release.
    * @throws IOException if an error occurs while closing or releasing the
    *                     index reader.
    */
   public static void closeOrRelease(final IndexReader reader) throws IOException
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
    * Returns a comparable for the internal <code>value</code>.
    *
    * @param value an internal value.
    * @return a comparable for the given <code>value</code>.
    */
   public static Comparable getComparable(ValueData value, int type) throws UnsupportedEncodingException,
      IllegalStateException, IOException, IllegalNameException, RepositoryException
   {
      switch (type)
      {
         case PropertyType.BINARY :
            return null;
         case PropertyType.BOOLEAN :
            return ComparableBoolean.valueOf(ValueDataUtil.getBoolean(value));
         case PropertyType.DATE :
            return new Long(ValueDataUtil.getDate(value).getTimeInMillis());
         case PropertyType.DOUBLE :
            return new Double(ValueDataUtil.getDouble(value));
         case PropertyType.LONG :
            return new Long(ValueDataUtil.getLong(value));
         case PropertyType.NAME :
            return new QPathEntry(InternalQName.parse(ValueDataUtil.getString(value)), 1);
         case PropertyType.PATH :
         case PropertyType.REFERENCE :
         case PropertyType.STRING :
            return ValueDataUtil.getString(value);
         default :
            return null;
      }
   }

   /**
    * Compares values <code>c1</code> and <code>c2</code>. If the
    * values have differing types, then the order is defined on
    * the type itself by calling <code>compareTo()</code> on the respective
    * type class names.
    *
    * @param c1 the first value.
    * @param c2 the second value.
    * @return a negative integer if <code>c1</code> should come before
    *         <code>c2</code><br> a positive integer if <code>c1</code>
    *         should come after <code>c2</code><br> <code>0</code> if they
    *         are equal.
    */
   public static int compare(Comparable c1, Comparable c2)
   {
      if (c1 == c2) // NOSONAR
      {
         return 0;
      }
      else if (c1 == null)
      {
         return -1;
      }
      else if (c2 == null)
      {
         return 1;
      }
      else if (c1.getClass() == c2.getClass())
      {
         return c1.compareTo(c2);
      }
      else
      {
         // differing types -> compare class names
         String name1 = c1.getClass().getName();
         String name2 = c2.getClass().getName();
         return name1.compareTo(name2);
      }
   }

   /**
    * Creates a regexp from <code>likePattern</code>.
    *
    * @param likePattern the pattern.
    * @return the regular expression <code>Pattern</code>.
    */
   public static Pattern createRegexp(String likePattern)
   {
      // - escape all non alphabetic characters
      // - escape constructs like \<alphabetic char> into \\<alphabetic char>
      // - replace non escaped _ % into . and .*
      StringBuffer regexp = new StringBuffer();
      boolean escaped = false;
      for (int i = 0; i < likePattern.length(); i++)
      {
         if (likePattern.charAt(i) == '\\')
         {
            if (escaped)
            {
               regexp.append("\\\\");
               escaped = false;
            }
            else
            {
               escaped = true;
            }
         }
         else
         {
            if (Character.isLetterOrDigit(likePattern.charAt(i)))
            {
               if (escaped)
               {
                  regexp.append("\\\\").append(likePattern.charAt(i));
                  escaped = false;
               }
               else
               {
                  regexp.append(likePattern.charAt(i));
               }
            }
            else
            {
               if (escaped)
               {
                  regexp.append('\\').append(likePattern.charAt(i));
                  escaped = false;
               }
               else
               {
                  switch (likePattern.charAt(i))
                  {
                     case '_' :
                        regexp.append('.');
                        break;
                     case '%' :
                        regexp.append(".*");
                        break;
                     default :
                        regexp.append('\\').append(likePattern.charAt(i));
                  }
               }
            }
         }
      }
      return Pattern.compile(regexp.toString(), Pattern.DOTALL);
   }

   /**
    * Returns length of the internal value.
    *
    * @param value a value.
   * @param propType 
    * @return the length of the internal value or <code>-1</code> if the length
    *         cannot be determined.
    */
   public static long getLength(ValueData value, int propType)
   {
      if (propType == PropertyType.BINARY)
      {
         return value.getLength();
      }
      else if (propType == PropertyType.NAME || propType == PropertyType.PATH)
      {
         return -1;
      }
      else
      {
         return value.toString().length();
      }
   }
}
