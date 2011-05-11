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
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version
 */
public class SystemViewContentExporter extends HandlingContentExporter
{

   /**
    * @param handler
    * @param session
    * @param dataManager
    * @param noRecurse
    * @throws NamespaceException
    * @throws RepositoryException
    */
   public SystemViewContentExporter(ContentHandler handler, ItemDataConsumer dataManager,
      NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse)
      throws NamespaceException, RepositoryException
   {
      super(handler, dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse);

   }

   /**
    * Return the current content handler
    */
   public ContentHandler getContentHandler()
   {
      return contentHandler;
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
         // set name of node as sv:name attribute
         AttributesImpl atts = new AttributesImpl();
         atts.addAttribute(getSvNamespaceUri(), "name", "sv:name", "CDATA", getExportName(node, false));

         contentHandler.startElement(getSvNamespaceUri(), "node", "sv:node", atts);
      }
      catch (SAXException e)
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
         // set name and type of property
         AttributesImpl atts = new AttributesImpl();
         atts.addAttribute(getSvNamespaceUri(), "name", "sv:name", "CDATA", getExportName(property, false));
         atts.addAttribute(getSvNamespaceUri(), "type", "sv:type", "CDATA", ExtendedPropertyType.nameFromValue(property
            .getType()));

         contentHandler.startElement(getSvNamespaceUri(), "property", "sv:property", atts);

         List<ValueData> values = property.getValues();
         for (ValueData valueData : values)
         {

            contentHandler.startElement(getSvNamespaceUri(), "value", "sv:value", new AttributesImpl());

            writeValueData(valueData, property.getType());
            contentHandler.endElement(getSvNamespaceUri(), "value", "sv:value");
         }
      }
      catch (SAXException e)
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
         contentHandler.endElement(getSvNamespaceUri(), "node", "sv:node");
      }
      catch (SAXException e)
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
         contentHandler.endElement(getSvNamespaceUri(), "property", "sv:property");
      }
      catch (SAXException e)
      {
         throw new RepositoryException(e);
      }
   }

   protected void writeValueData(ValueData data, int type) throws RepositoryException, IllegalStateException,
      IOException, SAXException
   {
      if (PropertyType.BINARY == type)
      {
         if (!isSkipBinary())
         {
            if (data.getLength() < 3 * 1024 * 3)
            {
               String charValue = getValueAsStringForExport(data, type);
               contentHandler.characters(charValue.toCharArray(), 0, charValue.length());
            }
            else
            {
               InputStream is = data.getAsStream();
               byte[] buffer = new byte[3 * 1024 * 3];
               int len;
               while ((len = is.read(buffer)) > 0)
               {
                  char[] charbuf1 = Base64.encode(buffer, 0, len, 0, "").toCharArray();
                  contentHandler.characters(charbuf1, 0, charbuf1.length);
               }
            }
         }
      }
      else
      {
         String charValue = getValueAsStringForExport(data, type);
         contentHandler.characters(charValue.toCharArray(), 0, charValue.length());
      }
   }
}
