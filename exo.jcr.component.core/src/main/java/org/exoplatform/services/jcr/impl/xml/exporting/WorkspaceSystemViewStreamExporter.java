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

import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: WorkspaceSystemViewStreamExporter.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */
public class WorkspaceSystemViewStreamExporter extends SystemViewStreamExporter
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceSystemViewStreamExporter");

   public WorkspaceSystemViewStreamExporter(XMLStreamWriter writer, ItemDataConsumer dataManager,
      NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse)
      throws NamespaceException, RepositoryException
   {
      super(writer, dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.xml.exporting.SystemViewStreamExporter#entering(org.exoplatform
    * .services.jcr.datamodel.NodeData, int)
    */
   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      super.entering(node, level);
      try
      {
         writer.writeAttribute(Constants.NS_EXO_PREFIX, Constants.NS_EXO_URI, Constants.EXO_ID, node.getIdentifier());

      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException(e);
      }
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.xml.exporting.SystemViewStreamExporter#entering(org.exoplatform
    * .services.jcr.datamodel.PropertyData, int)
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

         writer.writeAttribute(Constants.NS_EXO_PREFIX, Constants.NS_EXO_URI, Constants.EXO_ID, property
            .getIdentifier());

         if (property.isMultiValued())
         {
            writer.writeAttribute(Constants.NS_EXO_PREFIX, Constants.NS_EXO_URI, Constants.EXO_MULTIVALUED, "true");
         }

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
}
