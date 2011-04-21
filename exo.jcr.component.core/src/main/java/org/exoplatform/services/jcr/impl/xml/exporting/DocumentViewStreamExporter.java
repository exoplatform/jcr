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

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.util.StringConverter;

import java.io.IOException;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: DocumentViewStreamExporter.java 14244 2008-05-14 11:44:54Z ksm $
 */
public class DocumentViewStreamExporter extends StreamExporter
{

   public DocumentViewStreamExporter(XMLStreamWriter writer, ItemDataConsumer dataManager,
      NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse)
      throws NamespaceException, RepositoryException
   {
      super(writer, dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse, false);
   }

   /**
    * 
    * {@inheritDoc}
    */
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      try
      {
         if (!node.getQPath().getName().equals(Constants.JCR_XMLTEXT))
         {
            List<NodeData> nodes = dataManager.getChildNodesData(node);
            if (nodes.size() > 0)
            {
               writer.writeStartElement("", getExportName(node, true), "");
            }
            else
            {
               writer.writeEmptyElement("", getExportName(node, true), "");
            }
         }
         if (level == 0)
         {
            startPrefixMapping();

         }

      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * 
    * {@inheritDoc}
    */
   protected void entering(PropertyData property, int level) throws RepositoryException
   {
      InternalQName propName = property.getQPath().getName();
      try
      {
         if (propName.equals(Constants.JCR_XMLCHARACTERS))
         {
            writer
               .writeCharacters(new String(property.getValues().get(0).getAsByteArray(), Constants.DEFAULT_ENCODING));
         }
         else
         {

            //
            ItemData parentNodeData = dataManager.getItemData(property.getParentIdentifier());
            if (parentNodeData.getQPath().getName().equals(Constants.JCR_XMLTEXT))
            {
               return;
            }
            String strValue = "";

            for (ValueData valueData : property.getValues())
            {
               String strVal = getValueAsStringForExport(valueData, property.getType());
               if (strVal.equals(""))
               {
                  continue;
               }
               strValue +=
                  MULTI_VALUE_DELIMITER
                     + (property.getType() == PropertyType.BINARY ? strVal : StringConverter.normalizeString(strVal,
                        true));
            }

            writer.writeAttribute(getExportName(property, true), strValue.length() > 0 ? strValue.substring(1)
               : strValue);

         }
      }
      catch (IllegalStateException e)
      {
         throw new RepositoryException(e);
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }

   }

   /**
    * 
    * {@inheritDoc}
    */
   protected void leaving(NodeData node, int level) throws RepositoryException
   {

      try
      {
         if (!node.getQPath().getName().equals(Constants.JCR_XMLTEXT))
         {
            List<NodeData> nodes = dataManager.getChildNodesData(node);
            if (nodes.size() > 0)
            {
               writer.writeEndElement();
            }

         }
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * 
    * {@inheritDoc}
    */
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
   }
}
