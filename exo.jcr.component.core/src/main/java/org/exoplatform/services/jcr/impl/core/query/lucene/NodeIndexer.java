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
import org.apache.lucene.document.Fieldable;
import org.exoplatform.services.document.AdvancedDocumentReader;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.*;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.itemfilters.ExactQPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Creates a lucene <code>Document</code> object from a {@link javax.jcr.Node}.
 */
public class NodeIndexer
{

   /**
    * The logger instance for this class.
    */
   private static final Log             LOG                   = ExoLogger.getLogger("exo.jcr.component.core.NodeIndexer");

   /**
    * The default boost for a lucene field: 1.0f.
    */
   protected static final float DEFAULT_BOOST = 1.0f;

   /**
    * The <code>NodeState</code> of the node to index
    */
   protected final NodeDataIndexing node;

   /**
    * The persistent item state provider
    */
   protected final ItemDataConsumer stateProvider;

   /**
    * Namespace mappings to use for indexing. This is the internal
    * namespace mapping.
    */
   protected final NamespaceMappings mappings;

   /**
    * Name and Path resolver.
    */
   protected final LocationFactory resolver;

   /**
    * Content extractor.
    */
   protected final DocumentReaderService extractor;

   /**
    * The indexing configuration or <code>null</code> if none is available.
    */
   protected IndexingConfiguration indexingConfig;

   /**
    * If set to <code>true</code> the fulltext field is stored and and a term
    * vector is created with offset information.
    */
   protected boolean supportHighlighting = false;

   /**
    * Indicates index format for this node indexer.
    */
   protected IndexFormatVersion indexFormatVersion = IndexFormatVersion.V1;

   /**
    * List of {@link FieldNames#FULLTEXT} fields which should not be used in
    * an excerpt.
    */
   protected List<Fieldable> doNotUseInExcerpt = new ArrayList<Fieldable>();

   private int loadBatchingThreshold = -1;

   private boolean loadAllProperties;

   private boolean loadPropertyByName;

   /**
    * Creates a new node indexer.
    *
    * @param node          the node state to index.
    * @param stateProvider the persistent item state manager to retrieve properties.
    * @param mappings      internal namespace mappings.
    * @param extractor     content extractor
    */
   public NodeIndexer(NodeDataIndexing node, ItemDataConsumer stateProvider, NamespaceMappings mappings,
      DocumentReaderService extractor)
   {
      this.node = node;
      this.stateProvider = stateProvider;
      this.mappings = mappings;
      this.resolver = new LocationFactory(mappings);
      this.extractor = extractor;
   }

   /**
    * Returns the <code>NodeId</code> of the indexed node.
    * @return the <code>NodeId</code> of the indexed node.
    */
   public String getNodeId()
   {
      return node.getIdentifier();
   }

   /**
    * If set to <code>true</code> additional information is stored in the index
    * to support highlighting using the rep:excerpt pseudo property.
    *
    * @param b <code>true</code> to enable highlighting support.
    */
   public void setSupportHighlighting(boolean b)
   {
      supportHighlighting = b;
   }

   /**
    * Sets the index format version
    *
    * @param indexFormatVersion the index format version
    */
   public void setIndexFormatVersion(IndexFormatVersion indexFormatVersion)
   {
      this.indexFormatVersion = indexFormatVersion;
   }

   /**
    * Sets the indexing configuration for this node indexer.
    *
    * @param config the indexing configuration.
    */
   public void setIndexingConfiguration(IndexingConfiguration config)
   {
      this.indexingConfig = config;
   }

   /**
    * Sets the threshold from which we decide to load all the properties at the same time
    * instead of loading them individually. This is needed for performance reasons because
    * if you don't have a lot of properties it is faster to get them individually, starting
    * from a given limit it is better to load all the properties to avoid doing too many queries
    * 
    * @param loadBatchingThreshold the value of the threshold.
    */
   public void setLoadBatchingThreshold(int loadBatchingThreshold)
   {
      this.loadBatchingThreshold = loadBatchingThreshold;
   }

   /**
    * Indicates whether or not all the properties should be loaded using the method
    * {@link ItemDataConsumer#getChildPropertiesData(org.exoplatform.services.jcr.datamodel.NodeData)}
    */
   public void setLoadAllProperties(boolean loadAllProperties)
   {
      this.loadAllProperties = loadAllProperties;
   }

   /**
    * Indicates whether a property should be loaded by name or by id when we need to load a property one by one
    */
   public void setLoadPropertyByName(boolean loadPropertyByName)
   {
      this.loadPropertyByName = loadPropertyByName;
   }

   /**
    * Creates a lucene Document.
    *
    * @return the lucene Document with the index layout.
    * @throws RepositoryException if an error occurs while reading property
    *                             values from the <code>ItemStateProvider</code>.
    */
   protected Document createDoc() throws RepositoryException
   {
      doNotUseInExcerpt.clear();
      final Document doc = new Document();

      doc.setBoost(getNodeBoost());

      // special fields
      // UUID
      doc.add(new Field(FieldNames.UUID, node.getIdentifier(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

      if (indexingConfig != null && indexingConfig.isExcluded(node))
      {
         return doc;
      }

      try
      {
         if (node.getParentIdentifier() == null)
         {
            // root node
            doc.add(new Field(FieldNames.PARENT, "", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            addNodeName(doc, "", "");
         }
         else
         {
            addParentChildRelation(doc, node.getParentIdentifier());
         }
      }
      catch (NamespaceException e)
      {
         // will never happen, because this.mappings will dynamically add
         // unknown uri<->prefix mappings
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }

      if (indexFormatVersion.getVersion() >= IndexFormatVersion.V4.getVersion())
      {
         doc.add(new Field(FieldNames.INDEX, Integer.toString(node.getQPath().getIndex()), Field.Store.YES,
            Field.Index.NOT_ANALYZED_NO_NORMS));

         StringBuilder path = new StringBuilder(256);
         path.append(node.getParentIdentifier() == null ? "" : node.getParentIdentifier()).append('/')
            .append(node.getQPath().getName().getAsString());
         doc.add(new Field(FieldNames.PATH, path.toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
      }

      Collection<PropertyData> props = node.getChildPropertiesData();
      if (props == null)
      {
         if (loadAllProperties) 
         {
            props = stateProvider.getChildPropertiesData(node);
         }
         else
         {
            props = stateProvider.listChildPropertiesData(node);
            if (loadBatchingThreshold > -1 && props.size() > loadBatchingThreshold)
            {
               // The limit of properties to load individually has been reached so we perform
               // a batch loading
               List<QPathEntryFilter> filters = new ArrayList<QPathEntryFilter>(props.size());
               for (final PropertyData prop : props)
               {
                  filters.add(new ExactQPathEntryFilter(new QPathEntry(prop.getQPath().getName(), 0)));
               }
               // We use stateProvider.getChildPropertiesData(node, filters) instead of stateProvider.getChildPropertiesData(node)
               // because we want to get the properties from the cache if they are available
               props = stateProvider.getChildPropertiesData(node, filters);
            }
         }
      }

      for (final PropertyData prop : props)
      {

         // add each property to the _PROPERTIES_SET for searching
         // beginning with V2
         if (indexFormatVersion.getVersion() >= IndexFormatVersion.V2.getVersion())
         {
            addPropertyName(doc, prop.getQPath().getName());
         }
         addValues(doc, prop);
      }

      // now add fields that are not used in excerpt (must go at the end)
      for (Iterator<Fieldable> it = doNotUseInExcerpt.iterator(); it.hasNext();)
      {
         doc.add(it.next());
      }
      return doc;
   }

   /**
    * Adds a {@link FieldNames#MVP} field to <code>doc</code> with the resolved
    * <code>name</code> using the internal search index namespace mapping.
    *
    * @param doc  the lucene document.
    * @param name the name of the multi-value property.
    * @throws RepositoryException 
    */
   private void addMVPName(Document doc, InternalQName name) throws RepositoryException
   {
      try
      {
         String propName = resolver.createJCRName(name).getAsString();
         doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS,
            Field.TermVector.NO));
      }
      catch (NamespaceException e)
      {
         // will never happen, prefixes are created dynamically
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
   }

   /**
    * Adds a value to the lucene Document.
    *
    * @param doc   the document.
    * @param value the internal  value.
    * @param name  the name of the property.
    */
   private void addValues(final Document doc, final PropertyData prop) throws RepositoryException
   {
      int propType = prop.getType();
      String fieldName = resolver.createJCRName(prop.getQPath().getName()).getAsString();
      if (propType == PropertyType.BINARY)
      {
         InternalQName propName = prop.getQPath().getName();
         List<ValueData> data = null;
         if (node.getQPath().getName().equals(Constants.JCR_CONTENT) && isIndexed(propName))
         {

            // seems nt:file found, try for nt:resource props
            PropertyData pmime = node.getProperty(Constants.JCR_MIMETYPE.getAsString());
            if (pmime == null && !node.containAllProperties())
            {
               pmime =
                  (PropertyData)stateProvider.getItemData(node, new QPathEntry(Constants.JCR_MIMETYPE, 0),
                     ItemType.PROPERTY);
            }

            if (pmime != null && pmime.getValues() != null && !pmime.getValues().isEmpty())
            {
               // ok, have a reader
               // if the prop obtainer from cache it will contains a values,
               // otherwise read prop with values from DM
               PropertyData propData =
                  prop.getValues() != null && !prop.getValues().isEmpty() ? prop : ((PropertyData)stateProvider
                     .getItemData(node, new QPathEntry(Constants.JCR_DATA, 0), ItemType.PROPERTY));

               // index if have jcr:mimeType sibling for this binary property only
               try
               {
                  if (propData == null || (data = propData.getValues()) == null || data.isEmpty())
                  {
                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("No value found for the property located at " + prop.getQPath().getAsString());
                     }
                     return;
                  }

                  DocumentReader dreader =
                     extractor.getDocumentReader(ValueDataUtil.getString(pmime.getValues().get(0)));

                  // check the jcr:encoding property
                  PropertyData encProp = node.getProperty(Constants.JCR_ENCODING.getAsString());
                  if (encProp == null && !node.containAllProperties())
                  {
                     encProp =
                        (PropertyData)stateProvider.getItemData(node, new QPathEntry(Constants.JCR_ENCODING, 0),
                           ItemType.PROPERTY);
                  }

                  String encoding = null;
                  if (encProp != null && encProp.getValues() != null && !encProp.getValues().isEmpty())
                  {
                     // encoding parameter used
                     encoding = ValueDataUtil.getString(encProp.getValues().get(0));
                  }
                  else
                  {
                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("No encoding found for the node located at " + node.getQPath().getAsString());
                     }
                  }

                  if (dreader instanceof AdvancedDocumentReader)
                  {
                     // its a tika document reader that supports getContentAsReader
                     for (ValueData pvd : data)
                     {
                        // tikaDocumentReader will close inputStream, so no need to close it at finally 
                        // statement

                        InputStream is = null;
                        is = pvd.getAsStream();
                        Reader reader;
                        if (encoding != null)
                        {
                           reader = ((AdvancedDocumentReader)dreader).getContentAsReader(is, encoding);
                        }
                        else
                        {
                           reader = ((AdvancedDocumentReader)dreader).getContentAsReader(is);
                        }
                        doc.add(createFulltextField(reader));
                     }
                  }
                  else
                  {
                     // old-style document reader
                     for (ValueData pvd : data)
                     {
                        InputStream is = null;
                        try
                        {
                           is = pvd.getAsStream();
                           Reader reader;
                           if (encoding != null)
                           {
                              reader = new StringReader(dreader.getContentAsText(is, encoding));
                           }
                           else
                           {
                              reader = new StringReader(dreader.getContentAsText(is));
                           }
                           doc.add(createFulltextField(reader));
                        }
                        finally
                        {
                           try
                           {
                              is.close();
                           }
                           catch (Throwable e) //NOSONAR
                           {
                              if (LOG.isTraceEnabled())
                              {
                                 LOG.trace("An exception occurred: " + e.getMessage());
                              }
                           }
                        }
                     }
                  }

                  if (data.size() > 1)
                  {
                     // real multi-valued
                     addMVPName(doc, prop.getQPath().getName());
                  }

               }
               catch (DocumentReadException e)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("Cannot extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier() + "' : " + e, e);
                  }
                  else
                  {
                     LOG.warn("Cannot extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier());
                  }
               }
               catch (HandlerNotFoundException e)
               {
                  // no handler - no index
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("Can not indexing the document by path " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier() + "' : " + e, e);
                  }
               }
               catch (IOException e)
               {
                  // no data - no index
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("An IO exception occurred while trying to extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier() + "' : " + e, e);
                  }
                  else
                  {
                     LOG.warn("An IO exception occurred while trying to extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier());
                  }
               }
               catch (Exception e)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("An exception occurred while trying to extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier() + "' : " + e, e);
                  }
                  else
                  {
                     LOG.warn("An exception occurred while trying to extract the full text content of the property " + propData.getQPath().getAsString()
                        + ", propery id '" + propData.getIdentifier());
                  }
               }
            }
            else
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("no mime type found for the node located at " + node.getQPath().getAsString());
               }
            }
         }
      }
      else
      {
         try
         {
            // if the prop obtainer from cache it will contains a values, otherwise
            // read prop with values from DM
            // We access to the Item by path to avoid having to rebuild the path if needed in case
            // the indexingLoadBatchingThreshold is enabled only otherwise we get it from the id like
            // before
            boolean nonEmptyProp = prop.getValues() != null && !prop.getValues().isEmpty();
            PropertyData propData;
            try {
              propData = nonEmptyProp ? prop : (PropertyData)(loadPropertyByName
                    ? stateProvider.getItemData(node, new QPathEntry(prop.getQPath().getName(), 0), ItemType.PROPERTY)
                    : stateProvider.getItemData(prop.getIdentifier()));
            } catch (Exception e1) {
              if (LOG.isDebugEnabled()) {
                LOG.warn("Error reading value of property {}.", prop.getQPath().getAsString(), e1);
              } else {
                LOG.warn("Error reading value of property {}. Error: {}", prop.getQPath().getAsString(), e1.getMessage());
              }
              return;
            }

            List<ValueData> data;
            if (propData == null || (data = propData.getValues()) == null || data.isEmpty())
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.warn("null value found at property " + prop.getQPath().getAsString());
               }
               return;
            }

            InternalQName name = prop.getQPath().getName();
            for (ValueData value : data)
            {
               if (value == null) {
                 continue;
               }
               Object internalValue;
               switch (propType)
               {
                  case PropertyType.BOOLEAN :
                     if (isIndexed(name))
                     {
                        internalValue = ValueDataUtil.getBoolean(value);
                        if (internalValue == null) {
                          LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                          continue;
                        }
                        addBooleanValue(doc, fieldName, internalValue);
                     }
                     break;
                  case PropertyType.DATE :
                     if (isIndexed(name))
                     {
                       internalValue = ValueDataUtil.getDate(value);
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addCalendarValue(doc, fieldName, internalValue);
                     }
                     break;
                  case PropertyType.DOUBLE :
                     if (isIndexed(name))
                     {
                       internalValue = ValueDataUtil.getDouble(value);
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addDoubleValue(doc, fieldName, internalValue);
                     }
                     break;
                  case PropertyType.LONG :
                     if (isIndexed(name))
                     {
                       internalValue = ValueDataUtil.getLong(value);
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addLongValue(doc, fieldName, internalValue);
                     }
                     break;
                  case PropertyType.REFERENCE :
                     if (isIndexed(name))
                     {
                       internalValue = ValueDataUtil.getString(value);
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addReferenceValue(doc, fieldName, internalValue);
                     }
                     break;
                  case PropertyType.PATH :
                     if (isIndexed(name))
                     {
                       try {
                        internalValue = ValueDataUtil.getPath(value);
                      } catch (Exception e) {
                        LOG.warn("Error parsing PATH value of field {}, thus it will not be included in indexes", fieldName, e);
                        continue;
                      }
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addPathValue(doc, fieldName,
                           resolver.createJCRPath((QPath) internalValue).getAsString(false));
                     }
                     break;
                  case PropertyType.STRING :
                     if (isIndexed(name))
                     {
                        // never fulltext index jcr:uuid String
                        if (name.equals(Constants.JCR_UUID))
                        {
                          internalValue = ValueDataUtil.getString(value);
                          if (internalValue == null) {
                            LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                            continue;
                          }
                           addStringValue(doc, fieldName, internalValue, false, false, DEFAULT_BOOST,
                              true);
                        }
                        else
                        {
                          internalValue = ValueDataUtil.getString(value);
                          if (internalValue == null) {
                            LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                            continue;
                          }
                           addStringValue(doc, fieldName, internalValue, true,
                              isIncludedInNodeIndex(name), getPropertyBoost(name), useInExcerpt(name));
                        }
                     }
                     break;
                  case PropertyType.NAME :
                     // jcr:primaryType and jcr:mixinTypes are required for correct
                     // node type resolution in queries
                     if (isIndexed(name) || name.equals(Constants.JCR_PRIMARYTYPE)
                        || name.equals(Constants.JCR_MIXINTYPES))
                     {
                       try {
                         internalValue = ValueDataUtil.getName(value);
                       } catch (Exception e) {
                         LOG.warn("Error parsing NAME value of field {}, thus it will not be included in indexes", fieldName, e);
                         continue;
                       }
                       if (internalValue == null) {
                         LOG.warn("Field '{}' has a null value, thus it will not be included in indexes.", fieldName);
                         continue;
                       }
                        addNameValue(doc, fieldName, resolver.createJCRName((InternalQName) internalValue).getAsString());
                     }
                     break;
                  case ExtendedPropertyType.PERMISSION :
                     break;
                  default :
                     throw new IllegalArgumentException("illegal internal value type " + propType);
               }
               // add length
               // add not planed
               if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion())
               {
                  addLength(doc, fieldName, value, propType);
               }
            }
            if (data.size() > 1)
            {
               // real multi-valued
               addMVPName(doc, prop.getQPath().getName());
            }
         }
         catch (RepositoryException e)
         {
            LOG.error("Index of property value error. " + prop.getQPath().getAsString() + ".", e);
            throw new RepositoryException("Index of property value error. " + prop.getQPath().getAsString() + ". " + e,
               e);
         }
      }
   }

   /**
    * Adds the property name to the lucene _:PROPERTIES_SET field.
    *
    * @param doc  the document.
    * @param name the name of the property.
   * @throws RepositoryException 
    */
   private void addPropertyName(Document doc, InternalQName name) throws RepositoryException
   {
      String fieldName = name.getName();
      try
      {
         fieldName = resolver.createJCRName(name).getAsString();
      }
      catch (NamespaceException e)
      {
         // will never happen
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
   }

   /**
    * Adds the string representation of the boolean value to the document as
    * the named field.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    */
   protected void addBooleanValue(Document doc, String fieldName, Object internalValue)
   {
      doc.add(createFieldWithoutNorms(fieldName, internalValue.toString(), PropertyType.BOOLEAN));
   }

   /**
    * Creates a field of name <code>fieldName</code> with the value of <code>
    * internalValue</code>. The created field is indexed without norms.
    *
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    * @param propertyType  the property type.
    */
   protected Field createFieldWithoutNorms(String fieldName, String internalValue, int propertyType)
   {
      if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion())
      {
         Field field =
            new Field(FieldNames.PROPERTIES, new SingletonTokenStream(FieldNames.createNamedValue(fieldName,
               internalValue), propertyType));
         field.setOmitNorms(true);
         return field;
      }
      else
      {
         return new Field(FieldNames.PROPERTIES, FieldNames.createNamedValue(fieldName, internalValue), Field.Store.NO,
            Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO);
      }
   }

   /**
    * Adds the calendar value to the document as the named field. The calendar
    * value is converted to an indexable string value using the
    * {@link DateField} class.
    *
    * @param doc
    *            The document to which to add the field
    * @param fieldName
    *            The name of the field to add
    * @param internalValue
    *            The value for the field to add to the document.
    */
   protected void addCalendarValue(Document doc, String fieldName, Object internalValue)
   {
      Calendar value = (Calendar)internalValue;
      long millis = value.getTimeInMillis();
      try
      {
         doc.add(createFieldWithoutNorms(fieldName, DateField.timeToString(millis), PropertyType.DATE));
      }
      catch (IllegalArgumentException e)
      {
         LOG.warn("'{}' is outside of supported date value range.", new Date(value.getTimeInMillis()));
      }
   }

   /**
    * Adds the double value to the document as the named field. The double
    * value is converted to an indexable string value using the
    * {@link DoubleField} class.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    */
   protected void addDoubleValue(Document doc, String fieldName, Object internalValue)
   {
      double doubleVal = ((Double)internalValue).doubleValue();
      doc.add(createFieldWithoutNorms(fieldName, DoubleField.doubleToString(doubleVal), PropertyType.DOUBLE));
   }

   /**
    * Adds the long value to the document as the named field. The long
    * value is converted to an indexable string value using the {@link LongField}
    * class.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    */
   protected void addLongValue(Document doc, String fieldName, Object internalValue)
   {
      long longVal = ((Long)internalValue).longValue();
      doc.add(createFieldWithoutNorms(fieldName, LongField.longToString(longVal), PropertyType.LONG));
   }

   /**
    * Adds the reference value to the document as the named field. The value's
    * string representation is added as the reference data. Additionally the
    * reference data is stored in the index.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    */
   protected void addReferenceValue(Document doc, String fieldName, Object internalValue)
   {
      String uuid = internalValue.toString();
      doc.add(createFieldWithoutNorms(fieldName, uuid, PropertyType.REFERENCE));
      doc.add(new Field(FieldNames.PROPERTIES, FieldNames.createNamedValue(fieldName, uuid), Field.Store.YES,
         Field.Index.NO, Field.TermVector.NO));
   }

   /**
    * Adds the path value to the document as the named field. The path
    * value is converted to an indexable string value using the name space
    * mappings with which this class has been created.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param pathString    The value for the field to add to the document.
    */
   protected void addPathValue(Document doc, String fieldName, Object pathString)
   {

      doc.add(createFieldWithoutNorms(fieldName, pathString.toString(), PropertyType.PATH));
   }

   /**
    * Adds the string value to the document both as the named field and
    * optionally for full text indexing if <code>tokenized</code> is
    * <code>true</code>.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    * @param tokenized     If <code>true</code> the string is also tokenized
    *                      and fulltext indexed.
    */
   protected void addStringValue(Document doc, String fieldName, Object internalValue, boolean tokenized)
   {
      addStringValue(doc, fieldName, internalValue, tokenized, true, DEFAULT_BOOST, true);
   }

   /**
    * Adds the string value to the document both as the named field and
    * optionally for full text indexing if <code>tokenized</code> is
    * <code>true</code>.
    *
    * @param doc                The document to which to add the field
    * @param fieldName          The name of the field to add
    * @param internalValue      The value for the field to add to the
    *                           document.
    * @param tokenized          If <code>true</code> the string is also
    *                           tokenized and fulltext indexed.
    * @param includeInNodeIndex If <code>true</code> the string is also
    *                           tokenized and added to the node scope fulltext
    *                           index.
    * @param boost              the boost value for this string field.
    * @param useInExcerpt       If <code>true</code> the string may show up in
    *                           an excerpt.
    */
   protected void addStringValue(Document doc, String fieldName, Object internalValue, boolean tokenized,
      boolean includeInNodeIndex, float boost, boolean useInExcerpt)
   {

      // simple String
      String stringValue = (String)internalValue;
      doc.add(createFieldWithoutNorms(fieldName, stringValue, PropertyType.STRING));
      if (tokenized)
      {
         if (stringValue.length() == 0)
         {
            return;
         }
         // create fulltext index on property
         int idx = fieldName.indexOf(':');
         fieldName = fieldName.substring(0, idx + 1) + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
         Field f = new Field(fieldName, stringValue, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.NO);
         f.setBoost(boost);
         doc.add(f);

         if (includeInNodeIndex)
         {
            // also create fulltext index of this value
            boolean store = supportHighlighting && useInExcerpt;
            f = createFulltextField(stringValue, store, supportHighlighting);
            if (useInExcerpt)
            {
               doc.add(f);
            }
            else
            {
               doNotUseInExcerpt.add(f);
            }
         }
      }
   }

   /**
    * Adds the name value to the document as the named field. The name
    * value is converted to an indexable string treating the internal value
    * as a qualified name and mapping the name space using the name space
    * mappings with which this class has been created.
    *
    * @param doc           The document to which to add the field
    * @param fieldName     The name of the field to add
    * @param internalValue The value for the field to add to the document.
    */
   protected void addNameValue(Document doc, String fieldName, Object internalValue)
   {
      doc.add(createFieldWithoutNorms(fieldName, internalValue.toString(), PropertyType.NAME));
   }

   /**
    * Creates a fulltext field for the string <code>value</code>.
    *
    * @param value the string value.
    * @param store if the value of the field should be stored.
    * @param withOffsets if a term vector with offsets should be stored.
    * @return a lucene field.
    */
   protected Field createFulltextField(String value, boolean store, boolean withOffsets)
   {
      return new Field(FieldNames.FULLTEXT, value, store ? Field.Store.YES : Field.Store.NO, Field.Index.ANALYZED,
         withOffsets ? Field.TermVector.WITH_OFFSETS : Field.TermVector.NO);
   }

   /**
    * Creates a fulltext field for the reader <code>value</code>.
    *
    * @param value the reader value.
    * @return a lucene field.
    */
   protected Fieldable createFulltextField(Reader value)
   {
      if (supportHighlighting)
      {
         return new TextFieldExtractor(FieldNames.FULLTEXT, value, true, true);
      }
      else
      {
         return new TextFieldExtractor(FieldNames.FULLTEXT, value, false, false);
      }
   }

   /**
    * Returns <code>true</code> if the property with the given name should be
    * indexed.
    *
    * @param propertyName name of a property.
    * @return <code>true</code> if the property should be fulltext indexed;
    *         <code>false</code> otherwise.
    */
   protected boolean isIndexed(InternalQName propertyName)
   {
      if (indexingConfig == null)
      {
         return true;
      }
      else
      {
         return indexingConfig.isIndexed(node, propertyName);
      }
   }

   /**
    * Returns <code>true</code> if the property with the given name should also
    * be added to the node scope index.
    *
    * @param propertyName the name of a property.
    * @return <code>true</code> if it should be added to the node scope index;
    *         <code>false</code> otherwise.
    */
   protected boolean isIncludedInNodeIndex(InternalQName propertyName)
   {
      if (indexingConfig == null)
      {
         return true;
      }
      else
      {
         return indexingConfig.isIncludedInNodeScopeIndex(node, propertyName);
      }
   }

   /**
    * Returns <code>true</code> if the content of the property with the given
    * name should the used to create an excerpt.
    *
    * @param propertyName the name of a property.
    * @return <code>true</code> if it should be used to create an excerpt;
    *         <code>false</code> otherwise.
    */
   protected boolean useInExcerpt(InternalQName propertyName)
   {
      if (indexingConfig == null)
      {
         return true;
      }
      else
      {
         return indexingConfig.useInExcerpt(node, propertyName);
      }
   }

   /**
    * Returns the boost value for the given property name.
    *
    * @param propertyName the name of a property.
    * @return the boost value for the given property name.
    */
   protected float getPropertyBoost(InternalQName propertyName)
   {
      if (indexingConfig == null)
      {
         return DEFAULT_BOOST;
      }
      else
      {
         return indexingConfig.getPropertyBoost(node, propertyName);
      }
   }

   /**
    * @return the boost value for this {@link #node} state.
    */
   protected float getNodeBoost()
   {
      if (indexingConfig == null)
      {
         return DEFAULT_BOOST;
      }
      else
      {
         return indexingConfig.getNodeBoost(node);
      }
   }

   /**
    * Adds a {@link FieldNames#PROPERTY_LENGTHS} field to <code>document</code>
    * with a named length value.
    *
    * @param doc          the lucene document.
    * @param propertyName the property name.
    * @param value        the internal value.
   * @param propType 
    */
   protected void addLength(Document doc, String propertyName, ValueData value, int propType)
   {
      long length = Util.getLength(value, propType);
      if (length != -1)
      {
         doc.add(new Field(FieldNames.PROPERTY_LENGTHS, FieldNames.createNamedLength(propertyName, length),
            Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
      }
   }

   /**
    * Depending on the index format version adds one or two fields to the
    * document for the node name.
    *
    * @param doc the lucene document.
    * @param namespaceURI the namespace URI of the node name.
    * @param localName the local name of the node.
   * @throws RepositoryException 
    */
   protected void addNodeName(Document doc, String namespaceURI, String localName) throws RepositoryException
   {
      String name = mappings.getNamespacePrefixByURI(namespaceURI) + ":" + localName;
      doc.add(new Field(FieldNames.LABEL, name, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
      // as of version 3, also index combination of namespace URI and local name
      if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion())
      {
         doc.add(new Field(FieldNames.NAMESPACE_URI, namespaceURI, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
         doc.add(new Field(FieldNames.LOCAL_NAME, localName, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
      }
   }

   /**
    * Adds a parent child relation to the given <code>doc</code>.
    *
    * @param doc      the document.
    * @param parentId the id of the parent node.
    * @throws RepositoryException if the parent node does not have a child node
    *                             entry for the current node.
    */
   protected void addParentChildRelation(Document doc, String parentId) throws RepositoryException
   {
      doc.add(new Field(FieldNames.PARENT, parentId, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS,
         Field.TermVector.NO));
      //        NodeState parent = (NodeState) stateProvider.getItemState(parentId);
      //        ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
      //        if (child == null) {
      //            // this can only happen when jackrabbit
      //            // is running in a cluster.
      //            throw new RepositoryException(
      //                    "Missing child node entry for node with id: "
      //                    + node.getNodeId());
      //        }
      InternalQName name = node.getQPath().getName();
      addNodeName(doc, name.getNamespace(), name.getName());
   }
}
