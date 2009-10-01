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
import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;
import org.apache.lucene.search.SortField;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;

/**
 * Implements a <code>SortComparator</code> which knows how to sort on a lucene
 * field that contains values for multiple properties.
 */
public class SharedFieldSortComparator extends SortComparator
{

   /**
    * The name of the shared field in the lucene index.
    */
   private final String field;

   /**
    * The item state manager.
    */
   private final ItemDataConsumer ism;

   /**
    * LocationFactory.
    */
   private final LocationFactory locationFactory;

   /**
    * The index internal namespace mappings.
    */
   private final NamespaceMappings nsMappings;

   /**
    * Creates a new <code>SharedFieldSortComparator</code> for a given shared
    * field.
    *
    * @param fieldname the shared field.
    * @param ism       the item state manager of this workspace.
    * @param hmgr      the hierarchy manager of this workspace.
    * @param nsMappings the index internal namespace mappings.
    */
   public SharedFieldSortComparator(String fieldname, ItemDataConsumer ism, NamespaceMappings nsMappings)
   {
      this.field = fieldname;
      this.ism = ism;
      this.locationFactory = new LocationFactory(nsMappings);
      this.nsMappings = nsMappings;
   }

   /**
    * Creates a new <code>ScoreDocComparator</code> for an embedded
    * <code>propertyName</code> and a <code>reader</code>.
    *
    * @param reader the index reader.
    * @param relPath the relative path to the property to sort on as returned
    *          by {@link Path#getString()}.
    * @return a <code>ScoreDocComparator</code> for the
    * @throws IOException if an error occurs while reading from the index.
    */
   public ScoreDocComparator newComparator(IndexReader reader, String relPath) throws IOException
   {

      try
      {
         QPath p = locationFactory.parseJCRPath(relPath).getInternalPath();
         ScoreDocComparator simple = new SimpleScoreDocComparator(reader, nsMappings.translatePath(p));
         if (p.getEntries().length == 1)
         {
            return simple;
         }
         else
         {
            return new CompoundScoreDocComparator(reader, new ScoreDocComparator[]{simple,
               new RelPathScoreDocComparator(reader, p)});
         }
      }
      catch (IllegalNameException e)
      {
         throw Util.createIOException(e);
      }
      catch (RepositoryException e)
      {
         throw Util.createIOException(e);
      }
   }

   /**
    * @throws UnsupportedOperationException always.
    */
   protected Comparable getComparable(String termtext)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
    * that's the case calls this method recursively for each reader within the
    * multi index reader; otherwise the reader is simply added to the list.
    *
    * @param readers the list of index readers.
    * @param reader  the reader to check.
    */
   private static void getIndexReaders(List readers, IndexReader reader)
   {
      if (reader instanceof MultiIndexReader)
      {
         IndexReader[] r = ((MultiIndexReader)reader).getIndexReaders();
         for (int i = 0; i < r.length; i++)
         {
            getIndexReaders(readers, r[i]);
         }
      }
      else
      {
         readers.add(reader);
      }
   }

   /**
    * Abstract base class of {@link ScoreDocComparator} implementations.
    */
   abstract class AbstractScoreDocComparator implements ScoreDocComparator
   {

      /**
       * The index readers.
       */
      protected final List readers = new ArrayList();

      /**
       * The document number starts for the {@link #readers}.
       */
      protected final int[] starts;

      public AbstractScoreDocComparator(IndexReader reader) throws IOException
      {
         getIndexReaders(readers, reader);

         int maxDoc = 0;
         this.starts = new int[readers.size() + 1];

         for (int i = 0; i < readers.size(); i++)
         {
            IndexReader r = (IndexReader)readers.get(i);
            starts[i] = maxDoc;
            maxDoc += r.maxDoc();
         }
         starts[readers.size()] = maxDoc;
      }

      /**
       * Compares sort values of <code>i</code> and <code>j</code>. If the
       * sort values have differing types, then the sort order is defined on
       * the type itself by calling <code>compareTo()</code> on the respective
       * type class names.
       *
       * @param i first score doc.
       * @param j second score doc.
       * @return a negative integer if <code>i</code> should come before
       *         <code>j</code><br> a positive integer if <code>i</code>
       *         should come after <code>j</code><br> <code>0</code> if they
       *         are equal
       */
      public int compare(ScoreDoc i, ScoreDoc j)
      {
         return Util.compare(sortValue(i), sortValue(j));
      }

      public int sortType()
      {
         return SortField.CUSTOM;
      }

      /**
       * Returns the reader index for document <code>n</code>.
       *
       * @param n document number.
       * @return the reader index.
       */
      protected int readerIndex(int n)
      {
         int lo = 0;
         int hi = readers.size() - 1;

         while (hi >= lo)
         {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue)
            {
               hi = mid - 1;
            }
            else if (n > midValue)
            {
               lo = mid + 1;
            }
            else
            {
               while (mid + 1 < readers.size() && starts[mid + 1] == midValue)
               {
                  mid++;
               }
               return mid;
            }
         }
         return hi;
      }
   }

   /**
    * A score doc comparator that works for order by clauses with properties
    * directly on the result nodes.
    */
   private final class SimpleScoreDocComparator extends AbstractScoreDocComparator
   {

      /**
       * The term look ups of the index segments.
       */
      protected final SharedFieldCache.ValueIndex[] indexes;

      public SimpleScoreDocComparator(IndexReader reader, String propertyName) throws IOException
      {
         super(reader);
         this.indexes = new SharedFieldCache.ValueIndex[readers.size()];

         String namedValue = FieldNames.createNamedValue(propertyName, "");
         for (int i = 0; i < readers.size(); i++)
         {
            IndexReader r = (IndexReader)readers.get(i);
            indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, field, namedValue, SharedFieldSortComparator.this);
         }
      }

      /**
       * Returns the index term for the score doc <code>i</code>.
       *
       * @param i the score doc.
       * @return the sort value if available.
       */
      public Comparable sortValue(ScoreDoc i)
      {
         int idx = readerIndex(i.doc);
         return indexes[idx].getValue(i.doc - starts[idx]);
      }
   }

   /**
    * A score doc comparator that works with order by clauses that use a
    * relative path to a property to sort on.
    */
   private final class RelPathScoreDocComparator extends AbstractScoreDocComparator
   {

      private final QPath relPath;

      public RelPathScoreDocComparator(IndexReader reader, QPath relPath) throws IOException
      {
         super(reader);
         this.relPath = relPath;
      }

      /**
       * Returns the sort value for the given {@link ScoreDoc}. The value is
       * retrieved from the item state manager.
       *
       * @param i the score doc.
       * @return the sort value for the score doc.
       */
      public Comparable sortValue(ScoreDoc i)
      {
         try
         {
            int idx = readerIndex(i.doc);
            IndexReader reader = (IndexReader)readers.get(idx);
            Document doc = reader.document(i.doc - starts[idx], FieldSelectors.UUID);
            String uuid = doc.get(FieldNames.UUID);
            ItemData parent = ism.getItemData(uuid);
            if (!parent.isNode())
               throw new InvalidItemStateException();
            ItemData property = getItemData((NodeData)parent, relPath);
            if (property != null)
            {
               if (property.isNode())
                  throw new InvalidItemStateException();
               PropertyData propertyData = (PropertyData)property;
               List<ValueData> values = propertyData.getValues();
               if (values.size() > 0)
               {
                  return Util.getComparable(values.get(0), propertyData.getType());
               }
            }
            return null;
         }
         catch (Exception e)
         {
            e.printStackTrace();
            return null;
         }
      }
   }

   private ItemData getItemData(NodeData parent, QPathEntry name) throws RepositoryException
   {
      if (name.getName().equals(JCRPath.PARENT_RELPATH) && name.getNamespace().equals(Constants.NS_DEFAULT_URI))
      {
         if (parent.getIdentifier().equals(Constants.ROOT_UUID))
            return null;
         else
            return ism.getItemData(parent.getParentIdentifier());
      }

      return ism.getItemData(parent, name);

   }

   private ItemData getItemData(NodeData parent, QPath relPath) throws RepositoryException
   {

      QPathEntry[] relPathEntries =relPath.getEntries(); //relPath.getRelPath(relPath.getDepth());

      ItemData item = parent;
      for (int i = 0; i < relPathEntries.length; i++)
      {
         item = getItemData(parent, relPathEntries[i]);

         if (item == null)
            break;

         if (item.isNode())
            parent = (NodeData)item;
         else if (i < relPathEntries.length - 1)
            throw new IllegalPathException("Path can not contains a property as the intermediate element");
      }
      return item;

   }

   /**
    * Implements a compound score doc comparator that delegates to several
    * other comparators. The comparators are asked for a sort value in the
    * sequence they are passed to the constructor. The first non-null value
    * will be returned by {@link #sortValue(ScoreDoc)}.
    */
   private final class CompoundScoreDocComparator extends AbstractScoreDocComparator
   {

      private final ScoreDocComparator[] comparators;

      public CompoundScoreDocComparator(IndexReader reader, ScoreDocComparator[] comparators) throws IOException
      {
         super(reader);
         this.comparators = comparators;
      }

      /**
       * {@inheritDoc}
       */
      public Comparable sortValue(ScoreDoc i)
      {
         for (int j = 0; j < comparators.length; j++)
         {
            Comparable c = comparators[j].sortValue(i);
            if (c != null)
            {
               return c;
            }
         }
         return null;
      }
   }
}
