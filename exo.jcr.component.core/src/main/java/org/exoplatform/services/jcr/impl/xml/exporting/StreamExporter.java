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
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: StreamExporter.java 14244 2008-05-14 11:44:54Z ksm $
 */
public abstract class StreamExporter extends BaseXmlExporter
{

   protected final XMLStreamWriter writer;

   protected final boolean exportChildVersionHistory;

   public StreamExporter(XMLStreamWriter writer, ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry,
      ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse) throws NamespaceException,
      RepositoryException
   {
      this(writer, dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse, false);
   }

   public StreamExporter(XMLStreamWriter writer, ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry,
      ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse, boolean exportChildVersionHistory)
      throws NamespaceException, RepositoryException
   {
      super(dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse, noRecurse ? 1 : -1);
      this.writer = writer;
      this.exportChildVersionHistory = exportChildVersionHistory;
   }

   @Override
   public void export(NodeData node) throws RepositoryException, XMLStreamException
   {
      if (writer != null)
      {
         writer.writeStartDocument(Constants.DEFAULT_ENCODING, "1.0");
         node.accept(this);
         writer.writeEndDocument();
         writer.close();
      }
   }

   protected void startPrefixMapping() throws RepositoryException, XMLStreamException
   {
      String[] prefixes = getNamespaceRegistry().getPrefixes();
      for (String prefix : prefixes)
      {
         // skeep xml prefix
         if ((prefix == null) || (prefix.length() < 1) || prefix.equals(Constants.NS_XML_PREFIX))
         {
            continue;
         }
         writer.writeNamespace(prefix, getNamespaceRegistry().getURI(prefix));
      }
   };
}
