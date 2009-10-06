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

import javax.jcr.RepositoryException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ToStringUtils;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * <code>NameRangeQuery</code>...
 */
public class NameRangeQuery extends Query
{

   /**
    * The lower name. May be <code>null</code> if <code>upperName</code> is not
    * <code>null</code>.
    */
   private final InternalQName lowerName;

   /**
    * The upper name. May be <code>null</code> if <code>lowerName</code> is not
    * <code>null</code>.
    */
   private final InternalQName upperName;

   /**
    * If <code>true</code> the range interval is inclusive.
    */
   private final boolean inclusive;

   /**
    * The index format version.
    */
   private final IndexFormatVersion version;

   /**
    * The internal namespace mappings.
    */
   private final NamespaceMappings nsMappings;

   /**
    * Creates a new NameRangeQuery. The lower or the upper name may be
    * <code>null</code>, but not both!
    *
    * @param lowerName the lower name of the interval, or <code>null</code>
    * @param upperName the upper name of the interval, or <code>null</code>.
    * @param inclusive if <code>true</code> the interval is inclusive.
    * @param version the index format version.
    * @param nsMappings the internal namespace mappings.
    */
   public NameRangeQuery(InternalQName lowerName, InternalQName upperName, boolean inclusive,
      IndexFormatVersion version, NamespaceMappings nsMappings)
   {
      if (lowerName == null && upperName == null)
      {
         throw new IllegalArgumentException("At least one term must be non-null");
      }
      if (lowerName != null && upperName != null && !lowerName.getNamespace().equals(upperName.getNamespace()))
      {
         throw new IllegalArgumentException("Both names must have the same namespace URI");
      }
      this.lowerName = lowerName;
      this.upperName = upperName;
      this.inclusive = inclusive;
      this.version = version;
      this.nsMappings = nsMappings;
   }

   /**
    * {@inheritDoc}
    */
   public Query rewrite(IndexReader reader) throws IOException
   {
      if (version.getVersion() >= IndexFormatVersion.V3.getVersion())
      {
         RangeQuery localNames = new RangeQuery(getLowerLocalNameTerm(), getUpperLocalNameTerm(), inclusive);
         BooleanQuery query = new BooleanQuery();
         query.add(new JcrTermQuery(new Term(FieldNames.NAMESPACE_URI, getNamespaceURI())),
            BooleanClause.Occur.MUST);
         query.add(localNames, BooleanClause.Occur.MUST);
         return query.rewrite(reader);
      }
      else
      {
         return new RangeQuery(getLowerTerm(), getUpperTerm(), inclusive).rewrite(reader);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String toString(String field)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("name():");
      buffer.append(inclusive ? "[" : "{");
      buffer.append(lowerName != null ? lowerName.toString() : "null");
      buffer.append(" TO ");
      buffer.append(upperName != null ? upperName.toString() : "null");
      buffer.append(inclusive ? "]" : "}");
      buffer.append(ToStringUtils.boost(getBoost()));
      return buffer.toString();
   }

   //----------------------------< internal >----------------------------------

   /**
    * @return the namespace URI of this name query.
    */
   private String getNamespaceURI()
   {
      return lowerName != null ? lowerName.getNamespace() : upperName.getNamespace();
   }

   /**
    * @return the local name term of the lower name or <code>null</code> if no
    *         lower name is set.
    */
   private Term getLowerLocalNameTerm()
   {
      if (lowerName == null)
      {
         return null;
      }
      else
      {
         return new Term(FieldNames.LOCAL_NAME, lowerName.getName());
      }
   }

   /**
    * @return the local name term of the upper name or <code>null</code> if no
    *         upper name is set.
    */
   private Term getUpperLocalNameTerm()
   {
      if (upperName == null)
      {
         return null;
      }
      else
      {
         return new Term(FieldNames.LOCAL_NAME, upperName.getName());
      }
   }

   /**
    * @return the lower term. Must only be used for IndexFormatVersion &lt; 3.
    * @throws IOException if a name cannot be translated.
    */
   private Term getLowerTerm() throws IOException
   {
      try
      {
         String text;
         if (lowerName == null)
         {
            text = nsMappings.getNamespacePrefixByURI(upperName.getNamespace()) + ":";
         }
         else
         {
            text = nsMappings.translateName(lowerName);
         }
         return new Term(FieldNames.LABEL, text);
      }
      catch (RepositoryException e)
      {
         throw Util.createIOException(e);
      }
      catch (IllegalNameException e)
      {
         throw Util.createIOException(e);
      }
   }

   /**
    * @return the upper term. Must only be used for IndexFormatVersion &lt; 3.
    * @throws IOException if a name cannot be translated.
    */
   private Term getUpperTerm() throws IOException
   {
      try
      {
         String text;
         if (upperName == null)
         {
            text = nsMappings.getNamespacePrefixByURI(lowerName.getNamespace()) + ":\uFFFF";
         }
         else
         {
            text = nsMappings.translateName(upperName);
         }
         return new Term(FieldNames.LABEL, text);
      }
      catch (RepositoryException e)
      {
         throw Util.createIOException(e);
      }
      catch (IllegalNameException e)
      {
         throw Util.createIOException(e);
      }
   }
}
