/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.xml.exporting;

import org.apache.ws.commons.util.Base64;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.NodeDataOrderComparator;
import org.exoplatform.services.jcr.impl.dataflow.PropertyDataOrderComparator;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.util.ISO9075;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.xml.stream.XMLStreamException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ImportNodeData.java 11907 2008-03-13 15:36:21Z ksm $
 */
public abstract class BaseXmlExporter extends ItemDataTraversingVisitor
{
   /**
    * Empty namespace prefix.
    */
   public static final String DEFAULT_EMPTY_NAMESPACE_PREFIX = "jcr_default_empty" + "_namespace_prefix";

   /**
    * Multi-value delimiter.
    */
   public static final String MULTI_VALUE_DELIMITER = " ";

   /**
    * Root node name.
    */
   protected static final String JCR_ROOT = "jcr:root";

   /**
    * NamespacerRegistry.
    */
   private final NamespaceRegistry namespaceRegistry;

   /**
    * If noRecurse is true then only the node at absPath and its properties, but not its child nodes,
    * are serialized. If noRecurse is false then the entire subtree rooted at absPath is serialized.
    */
   private final boolean noRecurse;

   /**
    * Skip binary.
    */
   private final boolean skipBinary;

   /**
    * SV namespace uri.
    */
   private final String svNamespaceUri;

   /**
    * ValueFactory.
    */
   private final ValueFactoryImpl systemValueFactory;

   /**
    * @param dataManager - ItemDataConsumer
    * @param namespaceRegistry - NamespaceRegistry
    * @param systemValueFactory - default ValueFactory
    * @param skipBinary - If skipBinary is true then any properties of PropertyType.BINARY will be
    *          serialized as if they are empty.
    * @param maxLevel - maximum level
    * @param noRecurse - noRecurse value
    * @exception RepositoryException if an repository error occurs.
    */
   public BaseXmlExporter(ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry,
      ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse, int maxLevel)
      throws RepositoryException
   {
      super(dataManager, maxLevel);
      this.skipBinary = skipBinary;
      this.noRecurse = noRecurse;
      this.namespaceRegistry = namespaceRegistry;
      this.svNamespaceUri = namespaceRegistry.getURI("sv");

      this.systemValueFactory = systemValueFactory;

   }

   /**
    * @param node - exported node.
    * @throws Exception - exception.
    */
   public abstract void export(NodeData node) throws RepositoryException, SAXException, XMLStreamException;

   /**
    * @return - uri of the sv namespace.
    */
   public String getSvNamespaceUri()
   {
      return svNamespaceUri;
   }

   /**
    * @return - noRecurse.
    */
   public boolean isNoRecurse()
   {
      return noRecurse;
   }

   /**
    * @return - skip binary.
    */
   public boolean isSkipBinary()
   {
      return skipBinary;
   }

   /**
    * {@inheritDoc}
    */
   public void visit(NodeData node) throws RepositoryException
   {
      try
      {
         entering(node, currentLevel);
         if ((maxLevel == -1) || (currentLevel < maxLevel))
         {
            currentLevel++;

            List<PropertyData> properies = new ArrayList<PropertyData>(dataManager.getChildPropertiesData(node));
            // Sorting properties
            Collections.sort(properies, new PropertyDataOrderComparator());

            for (PropertyData data : properies)
            {
               InternalQName propName = data.getQPath().getName();

               // 7.3.3 Respecting Property Semantics
               // When an element or attribute representing such a property is
               // encountered, an implementation may either skip it or respect it.
               if (Constants.JCR_LOCKISDEEP.equals(propName) || Constants.JCR_LOCKOWNER.equals(propName))
               {
                  continue;
               }
               data.accept(this);
            }
            if (!isNoRecurse() && (currentLevel > 0))
            {
               List<NodeData> nodes = new ArrayList<NodeData>(dataManager.getChildNodesData(node));
               // Sorting nodes
               Collections.sort(nodes, new NodeDataOrderComparator());
               for (NodeData data : nodes)
               {
                  data.accept(this);
               }
            }
            currentLevel--;
         }
         leaving(node, currentLevel);
      }
      catch (RepositoryException re)
      {
         currentLevel = 0;
         throw re;
      }

   }

   /**
    * @param data - exported ItemData.
    * @param encode - is ISO9075 encode.
    * @return - exported item name.
    * @exception RepositoryException if an repository error occurs.
    */
   protected String getExportName(ItemData data, boolean encode) throws RepositoryException
   {
      String nodeName;
      QPath itemPath = data.getQPath();
      if (Constants.ROOT_PATH.equals(itemPath))
      {
         nodeName = JCR_ROOT;
      }
      else
      {

         InternalQName internalNodeName = itemPath.getName();
         if (encode)
         {
            internalNodeName = ISO9075.encode(itemPath.getName());
         }
         String prefix = namespaceRegistry.getPrefix(internalNodeName.getNamespace());
         nodeName = prefix.length() == 0 ? "" : prefix + ":";
         if ("".equals(itemPath.getName().getName()) && itemPath.isDescendantOf(Constants.EXO_NAMESPACES_PATH))
         {
            nodeName += DEFAULT_EMPTY_NAMESPACE_PREFIX;
         }
         else
         {
            nodeName += internalNodeName.getName();
         }

      }
      return nodeName;
   }

   /**
    * @param data - exported value data.
    * @param type - value type
    * @return - string representation of values prepared for export. Be attentive method encode
    *         binary values in memory. It is possible OutOfMemoryError on large Values.
    * @throws IllegalStateException
    * @throws IOException
    * @exception RepositoryException if an repository error occurs.
    * @exception IOException if an I/O error occurs.
    */
   protected String getValueAsStringForExport(ValueData data, int type) throws IOException, RepositoryException
   {
      String charValue = null;

      switch (type)
      {
         case PropertyType.BINARY :
            if (skipBinary)
            {
               charValue = "";
            }
            else
            {
               charValue = Base64.encode(data.getAsByteArray(), 0, (int)data.getLength(), 0, "");
            }
            break;
         case PropertyType.NAME :
         case PropertyType.DATE :
         case PropertyType.PATH :
            // TODO namespace mapping for values
            try
            {
               charValue = systemValueFactory.loadValue(data, type).getString();
            }
            catch (ValueFormatException e)
            {
               throw new RepositoryException(e);
            }
            catch (UnsupportedRepositoryOperationException e)
            {
               throw new RepositoryException(e);
            }
            break;
         default :
            charValue = ValueDataConvertor.readString(data);
            break;
      }
      return charValue;
   }

   public NamespaceRegistry getNamespaceRegistry()
   {
      return namespaceRegistry;
   }

   public ValueFactoryImpl getSystemValueFactory()
   {
      return systemValueFactory;
   }

}
