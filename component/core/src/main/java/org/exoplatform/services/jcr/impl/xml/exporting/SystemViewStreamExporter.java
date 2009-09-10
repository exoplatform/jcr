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
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SystemViewStreamExporter.java 14244 2008-05-14 11:44:54Z ksm $
 */
public class SystemViewStreamExporter extends StreamExporter
{

   private static final int BUFFER_SIZE = 3 * 1024 * 3;

   /**
    * @param writer
    * @param session
    * @param dataManager
    * @param maxLevel
    * @throws RepositoryException
    * @throws NamespaceException
    */
   public SystemViewStreamExporter(XMLStreamWriter writer, ItemDataConsumer dataManager,
      NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse)
      throws NamespaceException, RepositoryException
   {
      super(writer, dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor#entering(org.exoplatform.services
    * .jcr.datamodel.NodeData, int)
    */
   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      try
      {
         writer.writeStartElement(Constants.NS_SV_PREFIX, Constants.SV_NODE, getSvNamespaceUri());
         if (level == 0)
         {
            startPrefixMapping();
         }

         writer.writeAttribute(Constants.NS_SV_PREFIX, getSvNamespaceUri(), Constants.SV_NAME, getExportName(node,
            false));
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }

   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor#entering(org.exoplatform.services
    * .jcr.datamodel.PropertyData, int)
    */
   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {
      try
      {
         writer.writeStartElement(Constants.NS_SV_PREFIX, Constants.SV_PROPERTY, getSvNamespaceUri());

         writer.writeAttribute(Constants.NS_SV_PREFIX, getSvNamespaceUri(), Constants.SV_NAME, getExportName(property,
            false));

         writer.writeAttribute(Constants.NS_SV_PREFIX, getSvNamespaceUri(), Constants.SV_TYPE, ExtendedPropertyType
            .nameFromValue(property.getType()));

         List<ValueData> values = property.getValues();
         for (ValueData valueData : values)
         {

            writer.writeStartElement(Constants.NS_SV_PREFIX, Constants.SV_VALUE, getSvNamespaceUri());

            writeValueData(valueData, property.getType());

            writer.writeEndElement();
         }
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException("Can't export value to string: " + e.getMessage(), e);
      }
      catch (IllegalStateException e)
      {
         throw new RepositoryException("Can't export value to string: " + e.getMessage(), e);
      }
      catch (IOException e)
      {
         throw new RepositoryException("Can't export value to string: " + e.getMessage(), e);
      }

   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor#leaving(org.exoplatform.services
    * .jcr.datamodel.NodeData, int)
    */
   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
      try
      {
         writer.writeEndElement();
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }

   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor#leaving(org.exoplatform.services
    * .jcr.datamodel.PropertyData, int)
    */
   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
      try
      {
         writer.writeEndElement();
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }

   }

   protected void writeValueData(ValueData data, int type) throws RepositoryException, IllegalStateException,
      XMLStreamException, IOException
   {

      if (PropertyType.BINARY == type)
      {
         if (!isSkipBinary())
         {
            if (data.getLength() < BUFFER_SIZE)
            {
               String charValue = getValueAsStringForExport(data, type);
               writer.writeCharacters(charValue.toCharArray(), 0, charValue.length());
            }
            else
            {
               InputStream is = data.getAsStream();
               byte[] buffer = new byte[BUFFER_SIZE];
               int len;
               while ((len = is.read(buffer)) > 0)
               {
                  char[] charbuf1 = Base64.encode(buffer, 0, len, 0, "").toCharArray();
                  writer.writeCharacters(charbuf1, 0, charbuf1.length);
               }
            }
         }
      }
      else
      {
         String charValue = getValueAsStringForExport(data, type);
         writer.writeCharacters(charValue.toCharArray(), 0, charValue.length());
      }

   }
}
