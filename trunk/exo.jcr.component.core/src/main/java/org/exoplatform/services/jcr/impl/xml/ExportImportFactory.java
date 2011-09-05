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
package org.exoplatform.services.jcr.impl.xml;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.xml.exporting.BaseXmlExporter;
import org.exoplatform.services.jcr.impl.xml.exporting.DocumentViewContentExporter;
import org.exoplatform.services.jcr.impl.xml.exporting.DocumentViewStreamExporter;
import org.exoplatform.services.jcr.impl.xml.exporting.SystemViewContentExporter;
import org.exoplatform.services.jcr.impl.xml.exporting.SystemViewStreamExporter;
import org.exoplatform.services.jcr.impl.xml.exporting.WorkspaceSystemViewStreamExporter;
import org.exoplatform.services.jcr.impl.xml.importing.ContentHandlerImporter;
import org.exoplatform.services.jcr.impl.xml.importing.StreamImporter;
import org.exoplatform.services.jcr.impl.xml.importing.WorkspaceDataImporter;
import org.exoplatform.services.security.ConversationState;
import org.xml.sax.ContentHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ExportImportFactory.java 14244 2008-05-14 11:44:54Z ksm $
 */
public class ExportImportFactory
{

   /**
    * Create export visitor for given type of view.
    * 
    * @param type - 6.4 XML Mappings
    * @param contentHandler - for which will be generate SAX events
    * @param skipBinary - If skipBinary is true then any properties of
    *          PropertyType.BINARY will be serialized as if they are empty.
    * @param noRecurse - if noRecurse is false, the whole subtree are serialized.
    * @param dataManager - ItemDataConsumer
    * @param namespaceRegistry - NamespaceRegistry
    * @param systemValueFactory - default value factory
    * @return - visitor BaseXmlExporter.
    * @throws NamespaceException
    * @throws RepositoryException
    */
   public BaseXmlExporter getExportVisitor(XmlMapping type, ContentHandler contentHandler, boolean skipBinary,
      boolean noRecurse, ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry,
      ValueFactoryImpl systemValueFactory) throws NamespaceException, RepositoryException
   {

      if (type == XmlMapping.SYSVIEW)
      {
         return new SystemViewContentExporter(contentHandler, dataManager, namespaceRegistry, systemValueFactory,
            skipBinary, noRecurse);
      }
      else if (type == XmlMapping.DOCVIEW)
      {
         return new DocumentViewContentExporter(contentHandler, dataManager, namespaceRegistry, systemValueFactory,
            skipBinary, noRecurse);
      }
      return null;
   }

   /**
    * Create export visitor for given type of view.
    * 
    * @param type - 6.4 XML Mappings
    * @param stream - output result stream
    * @param skipBinary - If skipBinary is true then any properties of
    *          PropertyType.BINARY will be serialized as if they are empty.
    * @param noRecurse - if noRecurse is false, the whole subtree are serialized
    * @param dataManager - ItemDataConsumer
    * @param namespaceRegistry - NamespaceRegistry
    * @param systemValueFactory - default value factory
    * @return - visitor BaseXmlExporter.
    * @throws NamespaceException
    * @throws RepositoryException
    * @throws IOException
    */
   public BaseXmlExporter getExportVisitor(XmlMapping type, OutputStream stream, boolean skipBinary, boolean noRecurse,
      ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory)
      throws NamespaceException, RepositoryException, IOException
   {
      return getExportVisitor(type, stream, skipBinary, noRecurse, false, dataManager, namespaceRegistry,
         systemValueFactory);
   }

   /**
    * Create export visitor for given type of view.\
    * 
    * @param type - 6.4 XML Mappings
    * @param stream - output result stream
    * @param skipBinary- If skipBinary is true then any properties of
    *          PropertyType.BINARY will be serialized as if they are empty.
    * @param noRecurse- if noRecurse is false, the whole subtree are serialized
    * @param exportChildVersionHistory - does versioned child nodes version history must be exported
    *          (works ONLY with system view).
    * @param dataManager - ItemDataConsumer
    * @param namespaceRegistry - NamespaceRegistry
    * @param systemValueFactory - default value factory
    * @return - visitor BaseXmlExporter.
    * @throws NamespaceException
    * @throws RepositoryException
    * @throws IOException
    */
   public BaseXmlExporter getExportVisitor(XmlMapping type, OutputStream stream, boolean skipBinary, boolean noRecurse,
      boolean exportChildVersionHistory, ItemDataConsumer dataManager, NamespaceRegistry namespaceRegistry,
      ValueFactoryImpl systemValueFactory) throws NamespaceException, RepositoryException, IOException
   {

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLStreamWriter streamWriter;
      try
      {
         streamWriter = outputFactory.createXMLStreamWriter(stream, Constants.DEFAULT_ENCODING);
      }
      catch (XMLStreamException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }

      if (type == XmlMapping.SYSVIEW)
      {
         return new SystemViewStreamExporter(streamWriter, dataManager, namespaceRegistry, systemValueFactory,
            skipBinary, noRecurse, exportChildVersionHistory);
      }
      else if (type == XmlMapping.DOCVIEW)
      {
         return new DocumentViewStreamExporter(streamWriter, dataManager, namespaceRegistry, systemValueFactory,
            skipBinary, noRecurse);

      }
      else if (type == XmlMapping.BACKUP)
      {
         return new WorkspaceSystemViewStreamExporter(streamWriter, dataManager, namespaceRegistry, systemValueFactory,
            skipBinary, noRecurse);
      }
      return null;
   }

   /**
    * @param saveType
    * @param node
    * @param uuidBehavior
    * @param context
    * @return
    */
   public ContentHandler getImportHandler(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper,
      ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
      ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
      ConversationState userState, Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {

      return new ContentHandlerImporter(parent, uuidBehavior, dataKeeper, dataConsumer, ntManager, locationFactory,
         valueFactory, namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /**
    * @param saveType
    * @param node
    * @param uuidBehavior
    * @param context
    * @return
    */
   public StreamImporter getStreamImporter(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper,
      ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
      ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
      ConversationState userState, Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {

      return new StreamImporter(parent, uuidBehavior, dataKeeper, dataConsumer, ntManager, locationFactory,
         valueFactory, namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   public StreamImporter getWorkspaceImporter(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper,
      ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
      ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
      ConversationState userState, Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      return new WorkspaceDataImporter(parent, uuidBehavior, dataKeeper, dataConsumer, ntManager, locationFactory,
         valueFactory, namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }
}
