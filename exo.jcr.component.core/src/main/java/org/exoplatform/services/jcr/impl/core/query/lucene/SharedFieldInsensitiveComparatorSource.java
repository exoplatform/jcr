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
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.search.FieldComparator;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: SharedFieldInsensitiveComparatorSource.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SharedFieldInsensitiveComparatorSource extends SharedFieldComparatorSource
{

   /**
    * Constructor SharedFieldInsensitiveComparatorSource. 
    */
   public SharedFieldInsensitiveComparatorSource(String fieldname, ItemDataConsumer ism, NamespaceMappings nsMappings)
   {
      super(fieldname, ism, nsMappings);
   }

   /**
    * {@inheritDoc}
    */
   protected FieldComparator createSimpleComparator(int numHits, QPath path) throws IllegalNameException
   {
      return new SimpleFieldInsensitiveComparator(nsMappings.translatePath(path), field, numHits);
   }

   protected FieldComparator createCompoundComparator(int numHits, QPath path, SimpleFieldComparator simple)
   {
      return new CompoundScoreFieldInsensitiveComparator(new FieldComparator[]{simple,
         new RelPathFieldComparator(path, numHits)}, numHits);
   }

   static class SimpleFieldInsensitiveComparator extends SimpleFieldComparator
   {
      /**
       * Constructor SimpleFieldInsensitiveComparator. 
       */
      public SimpleFieldInsensitiveComparator(String propertyName, String fieldName, int numHits)
      {
         super(propertyName, fieldName, numHits);
      }

      /**
       * {@inheritDoc}
       */
      protected int compare(Comparable<?> val1, Comparable<?> val2)
      {
         val1 = makeInsensitiveValue(val1);
         val2 = makeInsensitiveValue(val2);

         return super.compare(val1, val2);
      }
   }

   static class CompoundScoreFieldInsensitiveComparator extends CompoundScoreFieldComparator
   {

      /**
       * Constructor CompoundScoreFieldInsensitiveComparator. 
       */
      public CompoundScoreFieldInsensitiveComparator(FieldComparator[] fieldComparators, int numHits)
      {
         super(fieldComparators, numHits);
      }

      /**
       * {@inheritDoc}
       */
      protected int compare(Comparable<?> val1, Comparable<?> val2)
      {
         val1 = makeInsensitiveValue(val1);
         val2 = makeInsensitiveValue(val2);

         return super.compare(val1, val2);
      }
   }

   private static Comparable<?>  makeInsensitiveValue(Comparable val)
   {
      if (val instanceof String)
      {
         return ((String)val).toUpperCase();
      }
      else if (val instanceof QPathEntry)
      {
         QPathEntry entry = (QPathEntry)val;
         return new QPathEntry(entry.getNamespace().toUpperCase(), entry.getName().toUpperCase(), entry.getIndex());
      }

      return val;
   }
}
