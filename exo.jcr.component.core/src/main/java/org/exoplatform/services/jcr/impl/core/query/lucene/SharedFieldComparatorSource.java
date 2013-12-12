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
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 18, 2012  
 */
public class SharedFieldComparatorSource extends FieldComparatorSource
{

   /**
    * The logger 
    */
   protected static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SharedFieldSortComparator");
   
   private static final long serialVersionUID = -5803240954874585429L;

   /**
    * The name of the shared field in the lucene index.
    */
   protected final String field;

   /**
    * The item state manager.
    */
   protected final ItemDataConsumer ism;

   /**
    * LocationFactory.
    */
   protected final LocationFactory locationFactory;

   /**
    * The index internal namespace mappings.
    */
   protected final NamespaceMappings nsMappings;

   /**
    * Creates a new <code>SharedFieldSortComparator</code> for a given shared
    * field.
    *
    * @param fieldname the shared field.
    * @param ism       the item state manager of this workspace.
    * @param hmgr      the hierarchy manager of this workspace.
    * @param nsMappings the index internal namespace mappings.
    */
   public SharedFieldComparatorSource(String fieldname, ItemDataConsumer ism, NamespaceMappings nsMappings)
   {
      this.field = fieldname;
      this.ism = ism;
      this.locationFactory = new LocationFactory(nsMappings);
      this.nsMappings = nsMappings;
   }

   /**
    * Create a new <code>FieldComparator</code> for an embedded <code>propertyName</code>
    * and a <code>reader</code>.
    *
    * @param propertyName the relative path to the property to sort on as returned
    *          by {@link org.apache.jackrabbit.spi.Path#getString()}.
    * @return a <code>FieldComparator</code>
    * @throws java.io.IOException if an error occurs
    */
   @Override
   public FieldComparator<?> newComparator(String propertyName, int numHits, int sortPos, boolean reversed)
      throws IOException
   {

      try
      {
         QPath path = locationFactory.parseJCRPath(propertyName).getInternalPath();
         SimpleFieldComparator simple = (SimpleFieldComparator)createSimpleComparator(numHits, path);
         if (path.getEntries().length == 1)
         {
            return simple;
         }
         else
         {
            return createCompoundComparator(numHits, path, simple);
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

   protected FieldComparator<?> createCompoundComparator(int numHits, QPath path, SimpleFieldComparator simple)
   {
      return new CompoundScoreFieldComparator(new FieldComparator[]{simple, new RelPathFieldComparator(path, numHits)},
         numHits);
   }

   protected FieldComparator<?> createSimpleComparator(int numHits, QPath path) throws IllegalNameException
   {
      return new SimpleFieldComparator(nsMappings.translatePath(path), field, numHits);
   }

   private ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      if (name.getName().equals(JCRPath.PARENT_RELPATH) && name.getNamespace().equals(Constants.NS_DEFAULT_URI))
      {
         if (parent.getIdentifier().equals(Constants.ROOT_UUID))
         {
            return null;
         }
         else
         {
            return ism.getItemData(parent.getParentIdentifier());
         }
      }

      return ism.getItemData(parent, name, itemType);

   }

   private ItemData getItemData(NodeData parent, QPath relPath, ItemType itemType) throws RepositoryException
   {

      QPathEntry[] relPathEntries = relPath.getEntries(); //relPath.getRelPath(relPath.getDepth());

      ItemData item = parent;
      for (int i = 0; i < relPathEntries.length; i++)
      {
         if (i == relPathEntries.length - 1)
         {
            item = getItemData(parent, relPathEntries[i], itemType);
         }
         else
         {
            item = getItemData(parent, relPathEntries[i], ItemType.UNKNOWN);
         }

         if (item == null)
         {
            break;
         }

         if (item.isNode())
         {
            parent = (NodeData)item;
         }
         else if (i < relPathEntries.length - 1)
         {
            throw new IllegalPathException("Path can not contains a property as the intermediate element");
         }
      }
      return item;
   }

   static class SimpleFieldComparator extends AbstractFieldComparator
   {

      /**
       * The term look ups of the index segments.
       */
      protected SharedFieldCache.ValueIndex[] indexes;

      /**
       * The name of the property
       */
      private final String propertyName;

      /**
       * The name of the field in the index
       */
      private final String fieldName;

      /**
       * Create a new instance of the <code>FieldComparator</code>.
       *
       * @param propertyName  the name of the property
       * @param fieldName     the name of the field in the index
       * @param numHits       the number of values 
       */
      public SimpleFieldComparator(String propertyName, String fieldName, int numHits)
      {
         super(numHits);
         this.propertyName = propertyName;
         this.fieldName = fieldName;
      }

      @Override
      public void setNextReader(IndexReader reader, int docBase) throws IOException
      {
         super.setNextReader(reader, docBase);

         indexes = new SharedFieldCache.ValueIndex[readers.size()];

         String namedValue = FieldNames.createNamedValue(propertyName, "");
         for (int i = 0; i < readers.size(); i++)
         {
            IndexReader r = readers.get(i);
            indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, fieldName, namedValue);
         }
      }

      @Override
      protected Comparable<?> sortValue(int doc)
      {
         int idx = readerIndex(doc);
         return indexes[idx].getValue(doc - starts[idx]);
      }
   }

   /**
    * Implements a compound <code>FieldComparator</code> which delegates to several
    * other comparators. The comparators are asked for a sort value in the
    * sequence they are passed to the constructor.
    */
   static class CompoundScoreFieldComparator extends AbstractFieldComparator
   {
      private final FieldComparator<?>[] fieldComparators;

      /**
       * Create a new instance of the <code>FieldComparator</code>.
       *
       * @param fieldComparators  delegates
       * @param numHits           the number of values
       */
      public CompoundScoreFieldComparator(FieldComparator<?>[] fieldComparators, int numHits)
      {
         super(numHits);
         this.fieldComparators = fieldComparators;
      }

      @Override
      public Comparable<?> sortValue(int doc)
      {
         for (FieldComparator<?> fieldComparator : fieldComparators)
         {
            if (fieldComparator instanceof FieldComparatorBase)
            {
               Comparable<?> c = ((FieldComparatorBase)fieldComparator).sortValue(doc);

               if (c != null)
               {
                  return c;
               }
            }
         }
         return null;
      }

      @Override
      public void setNextReader(IndexReader reader, int docBase) throws IOException
      {
         for (FieldComparator<?> fieldComparator : fieldComparators)
         {
            fieldComparator.setNextReader(reader, docBase);
         }
      }
   }

   /**
    * A <code>FieldComparator</code> which works with order by clauses that use a
    * relative path to a property to sort on.
    */
   final class RelPathFieldComparator extends AbstractFieldComparator
   {

      /**
       * Relative path to the property
       */
      private final QPath relPath;

      /**
       * Create a new instance of the <code>FieldComparator</code>.
       *
       * @param propertyName  relative path of the property
       * @param numHits       the number of values
       */
      public RelPathFieldComparator(QPath relPath, int numHits)
      {
         super(numHits);
         this.relPath = relPath;
      }

      @Override
      protected Comparable<?> sortValue(int doc)
      {
         try
         {
            int idx = readerIndex(doc);
            IndexReader reader = readers.get(idx);
            Document document = reader.document(doc - starts[idx], FieldSelectors.UUID);
            String uuid = document.get(FieldNames.UUID);
            ItemData parent = ism.getItemData(uuid);
            if (!parent.isNode())
            {
               throw new InvalidItemStateException();
            }
            ItemData property = getItemData((NodeData)parent, relPath, ItemType.PROPERTY);
            if (property != null)
            {
               if (property.isNode())
               {
                  throw new InvalidItemStateException();
               }
               PropertyData propertyData = (PropertyData)property;
               List<ValueData> values = propertyData.getValues();
               if (values.size() > 0)
               {
                  return Util.getComparable(values.get(0), propertyData.getType());
               }
            }
            return null;
         }
         catch (RepositoryException ignore)
         {
            LOG.error(ignore.getLocalizedMessage(), ignore);
         }
         catch (CorruptIndexException ignore)
         {
            LOG.error(ignore.getLocalizedMessage(), ignore);
         }
         catch (IOException ignore)
         {
            LOG.error(ignore.getLocalizedMessage(), ignore);
         }
         catch (IllegalStateException ignore)
         {
            LOG.error(ignore.getLocalizedMessage(), ignore);
         }
         catch (IllegalNameException ignore)
         {
            LOG.error(ignore.getLocalizedMessage(), ignore);
         }

         return null;
      }
   }

}
